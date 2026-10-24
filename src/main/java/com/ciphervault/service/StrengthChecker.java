package com.ciphervault.service;

public final class StrengthChecker {
    public static final String WEAK = "Weak";
    public static final String MEDIUM = "Medium";
    public static final String STRONG = "Strong";

    private StrengthChecker() {
    }

    /** One point each for 8+ characters, 12+ characters, a digit, an uppercase letter and a symbol. */
    public static int score(CharSequence password) {
        if (password == null || password.length() == 0) {
            return 0;
        }
        int score = 0;
        if (password.length() >= 8) {
            score++;
        }
        if (password.length() >= 12) {
            score++;
        }
        boolean digit = false;
        boolean upper = false;
        boolean symbol = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isDigit(c)) {
                digit = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (!Character.isLetter(c)) {
                symbol = true;
            }
        }
        if (digit) {
            score++;
        }
        if (upper) {
            score++;
        }
        if (symbol) {
            score++;
        }
        return score;
    }

    public static String check(CharSequence password) {
        switch (score(password)) {
            case 0:
            case 1:
            case 2:
                return WEAK;
            case 3:
                return MEDIUM;
            default:
                return STRONG;
        }
    }
}
