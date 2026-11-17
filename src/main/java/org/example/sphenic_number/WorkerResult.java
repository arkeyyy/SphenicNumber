package org.example.sphenic_number;

import java.util.List;
import java.util.Objects;

public record WorkerResult(NumberRange range, int checkedCount, List<Integer> matches) {
    public WorkerResult {
        Objects.requireNonNull(range, "range");
        matches = List.copyOf(matches);
        if (checkedCount < 0 || checkedCount > range.size() || matches.size() > checkedCount) {
            throw new IllegalArgumentException("Invalid result counts for the assigned range.");
        }
    }
}
