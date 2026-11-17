package org.example.sphenic_number;

import java.util.Locale;

/** Validated input and the application limits used by both the GUI and tests. */
public record RunParameters(int num, int cnt) {
    public static final int MAX_NUM = 10_000_000;
    public static final int MAX_THREADS = 64;

    public RunParameters {
        validateBounds(num, MAX_NUM, "Upper limit");
        validateBounds(cnt, MAX_THREADS, "Number of threads");
        if (cnt > num) {
            throw new IllegalArgumentException("Number of threads must not exceed the upper limit.");
        }
    }

    public static RunParameters parse(String numText, String cntText) {
        int num = parsePositiveInteger(numText, MAX_NUM, "Upper limit");
        int cnt = parsePositiveInteger(cntText, MAX_THREADS, "Number of threads");
        return new RunParameters(num, cnt);
    }

    private static int parsePositiveInteger(String text, int maximum, String label) {
        String trimmed = text == null ? "" : text.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Enter a value for " + label.toLowerCase(Locale.ROOT) + ".");
        }
        if (!trimmed.matches("[0-9]+")) {
            throw new IllegalArgumentException(label + " must be a positive whole number using digits only.");
        }
        try {
            int value = Integer.parseInt(trimmed);
            validateBounds(value, maximum, label);
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(boundsMessage(maximum, label));
        }
    }

    private static void validateBounds(int value, int maximum, String label) {
        if (value < 1 || value > maximum) {
            throw new IllegalArgumentException(boundsMessage(maximum, label));
        }
    }

    private static String boundsMessage(int maximum, String label) {
        return String.format(Locale.ROOT, "%s must be between 1 and %,d.", label, maximum);
    }
}
