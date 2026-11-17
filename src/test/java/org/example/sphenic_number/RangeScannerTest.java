package org.example.sphenic_number;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeScannerTest {
    @Test
    void producesSampleResultsAndCompleteMonotonicProgress() {
        List<Integer> progress = new ArrayList<>();
        WorkerResult result = RangeScanner.scan(new NumberRange(1, 100), () -> false, progress::add);
        assertEquals(List.of(30, 42, 66, 70, 78), result.matches());
        assertEquals(100, result.checkedCount());
        assertEquals(95, result.checkedCount() - result.matches().size());
        assertEquals(0, progress.getFirst());
        assertEquals(100, progress.getLast());
        for (int index = 1; index < progress.size(); index++) {
            assertTrue(progress.get(index) >= progress.get(index - 1));
            assertTrue(progress.get(index) <= result.range().size());
        }
    }

    @Test
    void scansInclusiveEndpointsAndEmptyResults() {
        assertEquals(List.of(), scan(1, 1).matches());
        assertEquals(List.of(), scan(1, 29).matches());
        assertEquals(List.of(30), scan(1, 30).matches());
        assertEquals(List.of(30), scan(30, 30).matches());
        assertEquals(List.of(42, 66, 70, 78), scan(42, 78).matches());
        assertEquals(1, scan(Integer.MAX_VALUE, Integer.MAX_VALUE).checkedCount());
    }

    @Test
    void parallelPartitionsProduceTheSameOrderedResultsAsASingleScan() throws Exception {
        int upperLimit = 25_000;
        for (int threadCount : new int[]{1, 3, 8}) {
            List<Callable<WorkerResult>> jobs = RangePartitioner.partition(upperLimit, threadCount).stream()
                    .<Callable<WorkerResult>>map(range -> () -> RangeScanner.scan(range, () -> false, ignored -> { }))
                    .toList();
            try (var executor = Executors.newFixedThreadPool(threadCount)) {
                List<Future<WorkerResult>> futures = executor.invokeAll(jobs);
                List<Integer> actual = new ArrayList<>();
                int totalChecked = 0;
                for (Future<WorkerResult> future : futures) {
                    WorkerResult result = future.get();
                    totalChecked += result.checkedCount();
                    actual.addAll(result.matches());
                }
                assertEquals(upperLimit, totalChecked);
                assertEquals(scan(1, upperLimit).matches(), actual);
            }
        }
    }

    @Test
    void cancellationStopsBeforeTheNextValueAndDoesNotReturnPartialResults() {
        AtomicInteger progress = new AtomicInteger();
        assertThrows(CancellationException.class,
                () -> RangeScanner.scan(new NumberRange(1, 10_000),
                        () -> progress.get() >= 25, progress::set));
        assertEquals(25, progress.get());
        assertThrows(CancellationException.class,
                () -> RangeScanner.scan(new NumberRange(1, 100), () -> true, ignored -> { }));
    }

    @Test
    void interruptionAlsoCancelsTheScan() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class,
                    () -> RangeScanner.scan(new NumberRange(1, 100), () -> false, ignored -> { }));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void workerResultsCannotBeChangedByAnotherThread() {
        List<Integer> source = new ArrayList<>(List.of(30));
        WorkerResult result = new WorkerResult(new NumberRange(1, 30), 30, source);
        source.clear();
        assertEquals(List.of(30), result.matches());
        assertThrows(UnsupportedOperationException.class, () -> result.matches().add(42));
    }

    private static WorkerResult scan(int start, int end) {
        return RangeScanner.scan(new NumberRange(start, end), () -> false, ignored -> { });
    }
}
