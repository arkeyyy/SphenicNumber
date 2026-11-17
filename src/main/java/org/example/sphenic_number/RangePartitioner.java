package org.example.sphenic_number;

import java.util.ArrayList;
import java.util.List;

/** Assigns contiguous ranges with increasing workload weights. */
public final class RangePartitioner {
    private RangePartitioner() {
    }

    public static List<NumberRange> partition(int num, int cnt) {
        if (num < 1 || cnt < 1 || cnt > num) {
            throw new IllegalArgumentException("Use a positive upper limit and between 1 and num threads.");
        }

        int[] sizes = new int[cnt];
        int remaining = num - cnt;
        long weightTotal = (long) cnt * (cnt + 1L) / 2;
        int allocated = 0;
        for (int index = 0; index < cnt; index++) {
            int extra = (int) ((long) remaining * (index + 1L) / weightTotal);
            sizes[index] = 1 + extra;
            allocated += extra;
        }

        int leftover = remaining - allocated;
        for (int index = cnt - 1; leftover > 0; index--, leftover--) {
            sizes[index]++;
        }

        // Equal workloads are unavoidable for one worker or one value per worker.
        if (cnt > 1 && num > cnt && sizes[0] == sizes[cnt - 1]) {
            sizes[0]--;
            sizes[cnt - 1]++;
        }

        List<NumberRange> ranges = new ArrayList<>(cnt);
        long next = 1;
        for (int size : sizes) {
            long end = next + size - 1;
            ranges.add(new NumberRange((int) next, (int) end));
            next = end + 1;
        }
        return List.copyOf(ranges);
    }
}
