package org.example.sphenic_number;

public record NumberRange(int startInclusive, int endInclusive) {
    public NumberRange {
        if (startInclusive < 1 || endInclusive < startInclusive) {
            throw new IllegalArgumentException("Invalid range: nonpositive or descending endpoints.");
        }
    }

    public int size() {
        return endInclusive - startInclusive + 1;
    }
}
