package org.example.sphenic_number;

/** A nonempty range of positive integers; both endpoints are included. */
public record NumberRange(int startInclusive, int endInclusive) {
    public NumberRange {
        if (startInclusive < 1 || endInclusive < startInclusive) {
            throw new IllegalArgumentException("A range must contain positive integers in ascending order.");
        }
    }

    public int size() {
        return endInclusive - startInclusive + 1;
    }
}
