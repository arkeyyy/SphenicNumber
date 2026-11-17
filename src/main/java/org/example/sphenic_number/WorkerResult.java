package org.example.sphenic_number;

import java.util.List;
import java.util.Objects;

/** An immutable snapshot of a worker's results, safe to pass between threads. */
public record WorkerResult(NumberRange range, int checkedCount, List<Integer> matches) {
    public WorkerResult {
        Objects.requireNonNull(range, "range");
        matches = List.copyOf(matches);
        if (checkedCount < 0 || checkedCount > range.size() || matches.size() > checkedCount) {
            throw new IllegalArgumentException("The result counts must fit the assigned range.");
        }
    }
}
