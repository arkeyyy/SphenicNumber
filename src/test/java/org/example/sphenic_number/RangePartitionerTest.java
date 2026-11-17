package org.example.sphenic_number;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangePartitionerTest {
    @Test
    void partitionsTheExampleDeterministically() {
        assertEquals(List.of(new NumberRange(1, 17), new NumberRange(18, 50),
                new NumberRange(51, 100)), RangePartitioner.partition(100, 3));
    }

    @Test
    void coversSmallInputsExactlyOnceWithUnequalWorkWhenPossible() {
        for (int num = 1; num <= 500; num++) {
            for (int cnt = 1; cnt <= Math.min(num, RunParameters.MAX_THREADS); cnt++) {
                verifyPartition(num, cnt);
            }
        }
    }

    @Test
    void avoidsOverflowAtLargeUpperLimits() {
        for (int num : new int[]{RunParameters.MAX_NUM, Integer.MAX_VALUE}) {
            for (int cnt : new int[]{1, 2, 3, 64}) {
                verifyPartition(num, cnt);
            }
        }
    }

    @Test
    void handlesUnavoidablyEqualWorkloads() {
        assertEquals(List.of(new NumberRange(1, 1)), RangePartitioner.partition(1, 1));
        assertEquals(List.of(new NumberRange(1, 100)), RangePartitioner.partition(100, 1));
        assertEquals(List.of(new NumberRange(1, 1), new NumberRange(2, 2), new NumberRange(3, 3)),
                RangePartitioner.partition(3, 3));
    }

    @Test
    void rejectsInvalidRangeAndPartitionInputs() {
        assertThrows(IllegalArgumentException.class, () -> new NumberRange(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new NumberRange(5, 4));
        for (int[] pair : new int[][]{{0, 1}, {-1, 1}, {10, 0}, {10, -1}, {3, 4}}) {
            assertThrows(IllegalArgumentException.class, () -> RangePartitioner.partition(pair[0], pair[1]));
        }
    }

    private static void verifyPartition(int num, int cnt) {
        List<NumberRange> ranges = RangePartitioner.partition(num, cnt);
        assertEquals(cnt, ranges.size());
        long next = 1;
        long total = 0;
        int previousSize = 0;
        for (NumberRange range : ranges) {
            assertEquals(next, range.startInclusive());
            assertTrue(range.size() > 0);
            assertTrue(range.size() >= previousSize);
            total += range.size();
            next = (long) range.endInclusive() + 1;
            previousSize = range.size();
        }
        assertEquals(num, total);
        assertEquals((long) num + 1, next);
        if (cnt > 1 && num > cnt) {
            assertFalse(ranges.stream().allMatch(range -> range.size() == ranges.getFirst().size()));
        }
    }
}
