package org.example.sphenic_number;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Exercises the loaded FXML and real button actions without a UI automation dependency. */
class SphenicControllerTest {
    private static final List<Integer> SAMPLE = List.of(30, 42, 66, 70, 78);
    private View view;

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            ready.countDown();
        });
        assertTrue(ready.await(10, TimeUnit.SECONDS), "JavaFX did not initialize");
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @BeforeEach
    void loadView() throws Exception {
        view = onFx(() -> {
            FXMLLoader loader = new FXMLLoader(SphenicApplication.class.getResource("sphenic-view.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 960, 720);
            root.applyCss();
            root.layout();
            return new View(root, scene, loader.getController(), new HashMap<>(loader.getNamespace()));
        });
    }

    @AfterEach
    void closeView() throws Exception {
        if (view != null) {
            onFx(() -> {
                view.controller().shutdown();
                return null;
            });
        }
        awaitNoWorkers();
    }

    @Test
    void defaultRunShowsSampleTotalsAndOneFinishedIndicatorPerThread() throws Exception {
        onFx(() -> {
            assertEquals("100", view.field("numField").getText());
            assertEquals("3", view.field("cntField").getText());
            view.button("startButton").fire();
            assertEquals(3, view.workers().getChildren().size());
            return null;
        });
        awaitCompletion();
        onFx(() -> {
            assertSampleResults();
            assertFalse(view.field("numField").isDisabled());
            assertFalse(view.field("cntField").isDisabled());
            assertTrue(view.button("cancelButton").isDisabled());
            var indicators = view.workers().lookupAll(".worker-progress");
            assertEquals(3, indicators.size());
            for (Node node : indicators) {
                assertEquals(1.0, ((ProgressIndicator) node).getProgress());
            }
            return null;
        });
    }

    @Test
    void invalidInputsShowAnErrorWithoutDiscardingCompletedResults() throws Exception {
        start("100", "3");
        awaitCompletion();
        onFx(() -> {
            for (String[] inputs : new String[][]{
                    {"", "3"}, {"1.5", "3"}, {"0", "3"}, {"10000001", "3"},
                    {"100", "65"}, {"2", "3"}, {"999999999999999999", "3"}
            }) {
                view.field("numField").setText(inputs[0]);
                view.field("cntField").setText(inputs[1]);
                view.button("startButton").fire();
                Label error = view.label("errorLabel");
                assertTrue(error.isVisible());
                assertTrue(error.isManaged());
                assertFalse(error.getText().isBlank());
                assertSampleResults();
                assertFalse(view.button("startButton").isDisabled());
                assertEquals(3, view.workers().getChildren().size());
            }
            return null;
        });
        start("30", "1");
        awaitCompletion();
        onFx(() -> {
            assertFalse(view.label("errorLabel").isVisible());
            assertEquals(List.of(30), view.matches().getItems());
            assertEquals("1", view.label("sphenicCountLabel").getText());
            assertEquals("29", view.label("nonSphenicCountLabel").getText());
            return null;
        });
    }

    @Test
    void emptyRangesShowZeroMatchesAndAnExplicitMessage() throws Exception {
        for (String[] inputs : new String[][]{{"1", "1"}, {"29", "3"}}) {
            start(inputs[0], inputs[1]);
            awaitCompletion();
            onFx(() -> {
                assertTrue(view.matches().getItems().isEmpty());
                assertEquals("0", view.label("sphenicCountLabel").getText());
                assertEquals(inputs[0], view.label("nonSphenicCountLabel").getText());
                assertTrue(view.label("resultsHintLabel").getText().contains("No sphenic numbers"));
                assertTrue(((Label) view.matches().getPlaceholder()).getText().contains("No sphenic"));
                return null;
            });
        }
    }

    @Test
    void cancellationRestoresInputsAndOldCallbacksDoNotOverwriteTheNextRun() throws Exception {
        onFx(() -> {
            setInputsAndStart("10000000", "1");
            assertTrue(view.button("startButton").isDisabled());
            assertTrue(view.field("numField").isDisabled());
            assertTrue(view.field("cntField").isDisabled());
            assertFalse(view.button("cancelButton").isDisabled());

            view.button("cancelButton").fire();
            assertFalse(view.button("startButton").isDisabled());
            assertFalse(view.field("numField").isDisabled());
            assertFalse(view.field("cntField").isDisabled());
            assertTrue(view.button("cancelButton").isDisabled());
            assertTrue(view.label("statusLabel").getText().contains("cancelled"));
            assertEquals("—", view.label("sphenicCountLabel").getText());
            assertEquals("—", view.label("nonSphenicCountLabel").getText());

            // Start immediately, before queued completion/cancellation events can be handled.
            setInputsAndStart("100", "3");
            return null;
        });
        awaitCompletion();
        awaitNoWorkers();
        onFx(() -> {
            assertSampleResults();
            assertEquals(3, view.workers().getChildren().size());
            assertTrue(view.label("statusLabel").getText().startsWith("All threads have finished."));
            return null;
        });
    }

    @Test
    void shuttingDownAnActiveSearchTerminatesItsWorkers() throws Exception {
        onFx(() -> {
            setInputsAndStart("10000000", "2");
            assertTrue(view.button("startButton").isDisabled());
            view.controller().shutdown();
            return null;
        });
        awaitNoWorkers();
        onFx(() -> {
            assertTrue(view.matches().getItems().isEmpty());
            assertEquals("—", view.label("sphenicCountLabel").getText());
            return null;
        });
    }

    @Test
    void minimumAndDefaultLayoutsKeepControlsAndManyWorkerRowsWithinWidth() throws Exception {
        start("1000", "64");
        awaitCompletion();
        onFx(() -> {
            assertEquals(64, view.workers().getChildren().size());
            for (double[] size : new double[][]{{744, 520}, {960, 720}}) {
                ((Region) view.root()).resize(size[0], size[1]);
                view.root().applyCss();
                view.root().layout();
                view.root().layout();
                for (String id : List.of("numField", "cntField", "startButton", "cancelButton",
                        "matchesList", "sphenicCountLabel", "nonSphenicCountLabel", "statusLabel")) {
                    assertWithinWidth(view.node(id), size[0]);
                }
                ScrollPane scroll = (ScrollPane) view.root().lookup(".worker-scroll");
                assertTrue(scroll.getViewportBounds().getWidth() > 0);
                assertTrue(view.workers().getWidth() <= scroll.getViewportBounds().getWidth() + 1,
                        "Worker content must fit the scroll viewport width");
                assertTrue(view.workers().getHeight() > scroll.getViewportBounds().getHeight(),
                        "Many worker rows should scroll vertically");
                for (Node row : view.workers().getChildren()) {
                    assertTrue(row.getBoundsInParent().getMaxX() <= view.workers().getWidth() + 1,
                            "Worker row must not extend beyond its container");
                    assertWithinWidth(row, size[0]);
                }
            }
            return null;
        });
    }

    private void start(String num, String cnt) throws Exception {
        onFx(() -> {
            setInputsAndStart(num, cnt);
            return null;
        });
    }

    private void setInputsAndStart(String num, String cnt) {
        view.field("numField").setText(num);
        view.field("cntField").setText(cnt);
        view.button("startButton").fire();
    }

    private void assertSampleResults() {
        assertEquals(SAMPLE, view.matches().getItems());
        assertEquals("5", view.label("sphenicCountLabel").getText());
        assertEquals("95", view.label("nonSphenicCountLabel").getText());
    }

    private void awaitCompletion() throws Exception {
        await(() -> {
            try {
                return onFx(() -> !view.button("startButton").isDisabled());
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }, "Search did not complete");
        onFx(() -> {
            assertTrue(view.label("statusLabel").getText().startsWith("All threads have finished."),
                    view.label("statusLabel").getText());
            return null;
        });
    }

    private static void awaitNoWorkers() throws Exception {
        await(() -> Thread.getAllStackTraces().keySet().stream()
                        .noneMatch(thread -> thread.isAlive() && thread.getName().startsWith("sphenic-worker-")),
                "Sphenic worker threads did not terminate");
    }

    private static void await(BooleanSupplier condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        fail(message);
    }

    private static void assertWithinWidth(Node node, double width) {
        Bounds bounds = node.localToScene(node.getLayoutBounds());
        assertTrue(bounds.getMinX() >= -1 && bounds.getMaxX() <= width + 1,
                () -> node.getId() + " exceeds the scene width: " + bounds);
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private record View(Parent root, Scene scene, SphenicController controller, Map<String, Object> namespace) {
        Node node(String id) {
            return (Node) namespace.get(id);
        }

        TextField field(String id) {
            return (TextField) namespace.get(id);
        }

        Button button(String id) {
            return (Button) namespace.get(id);
        }

        Label label(String id) {
            return (Label) namespace.get(id);
        }

        VBox workers() {
            return (VBox) namespace.get("workerRows");
        }

        @SuppressWarnings("unchecked")
        ListView<Integer> matches() {
            return (ListView<Integer>) namespace.get("matchesList");
        }
    }
}
