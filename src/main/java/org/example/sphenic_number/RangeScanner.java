package org.example.sphenic_number;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/** Checks a range without depending on JavaFX or sharing mutable state. */
public final class RangeScanner {
    private RangeScanner() {
    }

    /** Progress reports processed values, including zero and the final range size. */
    public static WorkerResult scan(NumberRange range, BooleanSupplier cancelled, IntConsumer progress) {
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(cancelled, "cancelled");
        Objects.requireNonNull(progress, "progress");
        checkCancellation(cancelled);
        progress.accept(0);

        List<Integer> matches = new ArrayList<>();
        int total = range.size();
        for (int checked = 0; checked < total; checked++) {
            checkCancellation(cancelled);
            int number = range.startInclusive() + checked;
            if (SphenicMath.isSphenic(number)) {
                matches.add(number);
            }
            progress.accept(checked + 1);
        }
        checkCancellation(cancelled);
        return new WorkerResult(range, total, matches);
    }

    private static void checkCancellation(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("The search was cancelled.");
        }
    }
}
