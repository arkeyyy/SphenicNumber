package org.example.sphenic_number;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunParametersTest {
    @Test
    void acceptsTrimmedWholeNumbersAndBoundaryValues() {
        assertEquals(new RunParameters(100, 3), RunParameters.parse(" 100 ", "\t3\n"));
        assertEquals(new RunParameters(100, 3), RunParameters.parse("000100", "03"));
        assertEquals(new RunParameters(1, 1), RunParameters.parse("1", "1"));
        assertEquals(new RunParameters(64, 64), RunParameters.parse("64", "64"));
        assertEquals(new RunParameters(10_000_000, 64), RunParameters.parse("10000000", "64"));
    }

    @Test
    void rejectsInvalidUpperLimitsWithFieldSpecificMessages() {
        for (String input : new String[]{null, "", " ", "0", "-1", "1.5", "abc", "1e3",
                "1,000", "10 00", "10000001", "2147483648", "9999999999999999999999999"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> RunParameters.parse(input, "1"), "Input: " + input);
            assertTrue(error.getMessage().toLowerCase(Locale.ROOT).contains("upper limit"));
        }
    }

    @Test
    void rejectsInvalidThreadCountsWithFieldSpecificMessages() {
        for (String input : new String[]{null, "", " ", "0", "-1", "1.5", "abc", "65", "2147483648"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> RunParameters.parse("100", input), "Input: " + input);
            assertTrue(error.getMessage().toLowerCase(Locale.ROOT).contains("number of threads"));
        }
        assertThrows(IllegalArgumentException.class, () -> RunParameters.parse("2", "3"));
    }

    @Test
    void directConstructionEnforcesTheSameLimits() {
        assertThrows(IllegalArgumentException.class, () -> new RunParameters(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new RunParameters(10_000_001, 1));
        assertThrows(IllegalArgumentException.class, () -> new RunParameters(100, 0));
        assertThrows(IllegalArgumentException.class, () -> new RunParameters(100, 65));
        assertThrows(IllegalArgumentException.class, () -> new RunParameters(2, 3));
    }
}
