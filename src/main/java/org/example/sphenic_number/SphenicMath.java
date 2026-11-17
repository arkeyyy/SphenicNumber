package org.example.sphenic_number;

public final class SphenicMath {
    private SphenicMath() {
    }

    public static boolean isSphenic(int number) {
        if (number < 30) {
            return false;
        }

        int remaining = number;
        int primeCount = 0;
        for (int divisor = 2; divisor <= remaining / divisor;
             divisor = divisor == 2 ? 3 : divisor + 2) {
            if (remaining % divisor != 0) {
                continue;
            }

            remaining /= divisor;
            if (remaining % divisor == 0) {
                return false;
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
