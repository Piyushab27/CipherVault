package com.ciphervault.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class PasswordGenerator {
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 32;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] LOWER = range('a', 26);
    private static final char[] UPPER = range('A', 26);
    private static final char[] DIGITS = range('0', 10);
    private static final char[] SYMBOLS = "!@#$%^&*()-_=+?".toCharArray();

    private PasswordGenerator() {
    }

    private static char[] range(char start, int count) {
        char[] chars = new char[count];
        for (int i = 0; i < count; i++) {
            chars[i] = (char) (start + i); // int back to char
        }
        return chars;
    }

    /** Letters and digits only. */
    public static String generate(int length) {
        return generate(length, false);
    }

    public static String generate(int length, boolean withSymbols) {
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException("Length must be " + MIN_LENGTH + " to " + MAX_LENGTH);
        }
        char[][] groups = withSymbols
                ? new char[][] {LOWER, UPPER, DIGITS, SYMBOLS}
                : new char[][] {LOWER, UPPER, DIGITS};
        return build(new StringBuilder(), length, groups).toString();
    }

    /**
     * Recursive: adds one random character and calls itself until the password is long enough.
     * When the characters left are only just enough to cover the groups still missing, it must pick from those,
     * so every password contains at least one lowercase, uppercase, digit (and symbol when asked).
     */
    private static StringBuilder build(StringBuilder sb, int length, char[][] groups) {
        if (sb.length() == length) {
            return sb;
        }
        List<char[]> missing = new ArrayList<>();
        for (char[] group : groups) {
            if (!containsAny(sb, group)) {
                missing.add(group);
            }
        }
        int remaining = length - sb.length();
        char[] source = !missing.isEmpty() && missing.size() >= remaining
                ? missing.get(RANDOM.nextInt(missing.size()))
                : groups[RANDOM.nextInt(groups.length)];
        sb.append(source[RANDOM.nextInt(source.length)]);
        return build(sb, length, groups);
    }

    private static boolean containsAny(CharSequence text, char[] group) {
        String allowed = new String(group);
        for (int i = 0; i < text.length(); i++) {
            if (allowed.indexOf(text.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }
}
