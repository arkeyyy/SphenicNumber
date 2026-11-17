package org.example.sphenic_number;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SphenicMathTest {
    @Test
    void recognizesProductsOfExactlyThreeDistinctPrimes() {
        for (int value : new int[]{30, 42, 66, 70, 78, 105, 231, 6_000_018}) {
            assertTrue(SphenicMath.isSphenic(value), "Expected sphenic: " + value);
        }
    }

    @Test
    void rejectsRepeatedPrimesAndWrongPrimeCounts() {
        for (int value : new int[]{Integer.MIN_VALUE, -30, 0, 1, 2, 6, 12, 24, 27,
                49, 60, 90, 120, 125, 210, 330, 343, Integer.MAX_VALUE}) {
            assertFalse(SphenicMath.isSphenic(value), "Expected non-sphenic: " + value);
        }
    }

    @Test
    void agreesWithIndependentPrimeTripleGeneration() {
        int limit = 5_000;
        boolean[] prime = new boolean[limit + 1];
        Arrays.fill(prime, true);
        prime[0] = false;
        prime[1] = false;
        for (int divisor = 2; divisor * divisor <= limit; divisor++) {
            if (prime[divisor]) {
                for (int multiple = divisor * divisor; multiple <= limit; multiple += divisor) {
                    prime[multiple] = false;
                }
            }
        }
        List<Integer> primes = new ArrayList<>();
        for (int value = 2; value <= limit; value++) {
            if (prime[value]) {
                primes.add(value);
            }
        }

        boolean[] expected = new boolean[limit + 1];
        for (int first = 0; first < primes.size() - 2; first++) {
            for (int second = first + 1; second < primes.size() - 1; second++) {
                long pair = (long) primes.get(first) * primes.get(second);
                if (pair * primes.get(second + 1) > limit) {
                    break;
                }
                for (int third = second + 1; third < primes.size(); third++) {
                    long product = pair * primes.get(third);
                    if (product > limit) {
                        break;
                    }
                    expected[(int) product] = true;
                }
            }
        }
        for (int value = 1; value <= limit; value++) {
            assertEquals(expected[value], SphenicMath.isSphenic(value), "Mismatch for " + value);
        }
    }
}
