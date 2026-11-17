package org.example.sphenic_number;

/** Number-theory operations independent of the user interface. */
public final class SphenicMath {
    private SphenicMath() {
    }

    /** Returns whether {@code number} is the product of exactly three distinct primes. */
    public static boolean isSphenic(int number) {
        if (number < 30) {
            return false;
        }

        int remaining = number;
        int primeCount = 0;
        // Division avoids overflowing divisor * divisor for large input values.
        for (int divisor = 2; divisor <= remaining / divisor;
             divisor = divisor == 2 ? 3 : divisor + 2) {
            if (remaining % divisor != 0) {
                continue;
            }

            remaining /= divisor;
            if (remaining % divisor == 0) {
                return false; // Any repeated prime disqualifies the number.
            }
            primeCount++;
            if (primeCount > 3) {
                return false;
            }
        }

        if (remaining > 1) {
            primeCount++;
        }
        return primeCount == 3;
    }
}
