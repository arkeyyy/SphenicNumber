package org.example.sphenic_number;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SphenicController {
    @FXML private TextField numField;
    @FXML private TextField cntField;
    @FXML private Button startButton;
    @FXML private Button cancelButton;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;
    @FXML private TilePane workerRows;
    @FXML private ListView<Integer> matchesList;
    @FXML private Label sphenicCountLabel;
    @FXML private Label nonSphenicCountLabel;
    @FXML private Label resultsTitleLabel;
    @FXML private Label resultsHintLabel;

    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.US);
    private RunState currentRun;
    private boolean closed;

    @FXML
    private void initialize() {
        resultsHintLabel.visibleProperty().bind(resultsHintLabel.textProperty().isNotEmpty());
        resultsHintLabel.managedProperty().bind(resultsHintLabel.visibleProperty());
        matchesList.setPlaceholder(new Label("No results yet"));
        matchesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : numbers.format(item));
            }
        });
    }

    @FXML
    private void onStart() {
        if (closed || currentRun != null) {
            return;
        }
        RunParameters parameters;
        try {
            parameters = RunParameters.parse(numField.getText(), cntField.getText());
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
            if (exception.getMessage().toLowerCase(Locale.ROOT).contains("upper limit")
                    && !exception.getMessage().startsWith("Number of threads")) {
                numField.requestFocus();
            } else {
                cntField.requestFocus();
            }
            return;
        }

        clearError();
        matchesList.getItems().clear();
        matchesList.setPlaceholder(new Label("Search in progress…"));
        workerRows.getChildren().clear();
        sphenicCountLabel.setText("—");
        nonSphenicCountLabel.setText("—");
        resultsTitleLabel.setText("Sphenic numbers");
        resultsHintLabel.setText("Range 1–" + numbers.format(parameters.num()));
        statusLabel.setText("Checking 1–" + numbers.format(parameters.num()) + " with "
                + parameters.cnt() + (parameters.cnt() == 1 ? " thread…" : " threads…"));
        setRunning(true);

        List<NumberRange> ranges = RangePartitioner.partition(parameters.num(), parameters.cnt());
        RunState run = new RunState(parameters);
        currentRun = run;
        try {
            for (int index = 0; index < ranges.size(); index++) {
                NumberRange range = ranges.get(index);
                WorkerView view = new WorkerView(index + 1, range);
                run.views.add(view);
                workerRows.getChildren().add(view.row);
                Task<WorkerResult> task = createWorker(run, index, range, view);
                run.tasks.add(task);
            }
            for (Task<?> task : run.tasks) {
                run.executor.execute(task);
            }
        } catch (RuntimeException exception) {
            failRun(run, exception);
        }
    }

    private Task<WorkerResult> createWorker(RunState run, int index, NumberRange range,
                                           WorkerView view) {
        Task<WorkerResult> task = new Task<>() {
            @Override
            protected WorkerResult call() {
                long[] lastUpdate = {0};
                return RangeScanner.scan(range, this::isCancelled, processed -> {
                    long now = System.nanoTime();
                    if (processed == 0 || processed == range.size()
                            || now - lastUpdate[0] >= 50_000_000L) {
                        updateProgress(processed, range.size());
                        lastUpdate[0] = now;
                    }
                });
            }
        };
        task.setOnRunning(event -> {
            if (currentRun == run) {
                view.status.setText("Running");
            }
        });
        task.progressProperty().addListener((observable, oldValue, newValue) -> {
            if (currentRun == run && !view.finished) {
                double progress = Math.max(0, Math.min(1, newValue.doubleValue()));
                view.progress.setProgress(progress);
            }
        });
        task.setOnSucceeded(event -> {
            if (currentRun != run) {
                return;
            }
            view.finished = true;
            view.progress.setProgress(1);
            view.status.setText("Done");
            run.results.set(index, task.getValue());
            run.completed++;
            if (run.completed == run.parameters.cnt()) {
                aggregate(run);
            }
        });
        task.setOnFailed(event -> {
            if (currentRun == run) {
                failRun(run, task.getException());
                view.status.setText("Failed");
            }
        });
        task.setOnCancelled(event -> {
            if (currentRun == run) {
                cancelRun(run, "Search cancelled.");
            }
        });
        return task;
    }

    private void aggregate(RunState run) {
        statusLabel.setText("All threads finished. Preparing results…");
        List<WorkerResult> results = List.copyOf(run.results);
        Task<List<Integer>> task = new Task<>() {
            @Override
            protected List<Integer> call() {
                int size = results.stream().mapToInt(result -> result.matches().size()).sum();
                List<Integer> matches = new ArrayList<>(size);
                for (WorkerResult result : results) {
                    if (isCancelled()) {
                        throw new CancellationException();
                    }
                    matches.addAll(result.matches());
                }
                return matches;
            }
        };
        task.setOnSucceeded(event -> {
            if (currentRun != run) {
                return;
            }
            List<Integer> matches = task.getValue();
            matchesList.setItems(FXCollections.observableList(matches));
            matchesList.setPlaceholder(new Label("No sphenic numbers in this range"));
            sphenicCountLabel.setText(numbers.format(matches.size()));
            nonSphenicCountLabel.setText(numbers.format(run.parameters.num() - matches.size()));
            resultsHintLabel.setText(matches.isEmpty()
                    ? "No sphenic numbers found from 1 to " + numbers.format(run.parameters.num()) + "."
                    : "In ascending order · Range 1–" + numbers.format(run.parameters.num()));
            statusLabel.setText("All threads have finished. " + numbers.format(run.parameters.num())
                    + " numbers checked.");
            currentRun = null;
            setRunning(false);
        });
        task.setOnFailed(event -> failRun(run, task.getException()));
        task.setOnCancelled(event -> {
            if (currentRun == run) {
                cancelRun(run, "Search cancelled.");
            }
        });
        run.tasks.add(task);
        try {
            run.executor.execute(task);
            run.executor.shutdown();
        } catch (RuntimeException exception) {
            failRun(run, exception);
        }
    }

    @FXML
    private void onCancel() {
        if (currentRun != null) {
            cancelRun(currentRun, "Search cancelled.");
        }
    }

    private void cancelRun(RunState run, String message) {
        if (currentRun != run) {
            return;
        }
        currentRun = null;
        run.cancel();
        for (WorkerView view : run.views) {
            if (!view.finished) {
                view.status.setText("Cancelled");
            }
        }
        matchesList.setPlaceholder(new Label("Search cancelled"));
        resultsHintLabel.setText("No final totals.");
        statusLabel.setText(message);
        setRunning(false);
    }

    private void failRun(RunState run, Throwable exception) {
        if (currentRun != run) {
            return;
        }
        cancelRun(run, "Search failed.");
        matchesList.setPlaceholder(new Label("Search could not finish"));
        showError("The search could not finish.");
        exception.printStackTrace(System.err);
    }

    public void shutdown() {
        closed = true;
        if (currentRun != null) {
            RunState run = currentRun;
            currentRun = null;
            run.cancel();
        }
    }

    private void setRunning(boolean running) {
        numField.setDisable(running);
        cntField.setDisable(running);
        startButton.setDisable(running);
        cancelButton.setDisable(!running);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private final class WorkerView {
        private final VBox row;
        private final ProgressIndicator progress = new ProgressIndicator(0);
        private final Label status = new Label("Waiting");
        private boolean finished;

        private WorkerView(int index, NumberRange range) {
            progress.getStyleClass().add("worker-progress");
            progress.setMinSize(48, 48);
            progress.setPrefSize(48, 48);
            progress.setMaxSize(48, 48);
            progress.setAccessibleText("Thread " + index + " progress");
            Label title = new Label("Thread " + index);
            title.getStyleClass().add("worker-title");
            Label rangeLabel = new Label(numbers.format(range.startInclusive()) + " to "
                    + numbers.format(range.endInclusive()));
            rangeLabel.getStyleClass().add("worker-range");
            rangeLabel.setWrapText(true);
            rangeLabel.setMaxWidth(120);
            rangeLabel.setAlignment(Pos.CENTER);
            Label sizeLabel = new Label(numbers.format(range.size())
                    + (range.size() == 1 ? " number" : " numbers"));
            sizeLabel.getStyleClass().add("worker-range");
            status.getStyleClass().add("worker-status");
            status.setWrapText(true);
            status.setMaxWidth(120);
            status.setAlignment(Pos.CENTER);
            row = new VBox(4, title, progress, rangeLabel, sizeLabel, status);
            row.setPrefWidth(120);
            row.setAlignment(Pos.CENTER);
            row.getStyleClass().add("worker-row");
        }
    }

    private static final class RunState {
        private final RunParameters parameters;
        private final ExecutorService executor;
        private final List<Task<?>> tasks = new ArrayList<>();
        private final List<WorkerResult> results;
        private final List<WorkerView> views = new ArrayList<>();
        private int completed;

        private RunState(RunParameters parameters) {
            this.parameters = parameters;
            this.results = new ArrayList<>(Collections.nCopies(parameters.cnt(), null));
            AtomicInteger threadNumber = new AtomicInteger();
            executor = Executors.newFixedThreadPool(parameters.cnt(), runnable -> {
                Thread thread = new Thread(runnable, "sphenic-worker-" + threadNumber.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
        }

        private void cancel() {
            tasks.forEach(task -> task.cancel(true));
            executor.shutdownNow();
        }
    }
}
