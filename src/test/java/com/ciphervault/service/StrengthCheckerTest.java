package com.ciphervault.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StrengthCheckerTest {
    @Test
    void abc123IsWeak() {
        assertEquals("Weak", StrengthChecker.check("abc123"));
    }

    @Test
    void hello2026IsMedium() {
        assertEquals("Medium", StrengthChecker.check("Hello2026"));
    }

    @Test
    void longMixedPasswordsAreStrong() {
        assertEquals("Strong", StrengthChecker.check("T7#qv!Lm92@x"));
        assertEquals("Strong", StrengthChecker.check("Insta#2026x"));
    }

    /** White-box: one case per branch of score(). */
    @Test
    void everyBranchOfScore() {
        assertEquals(0, StrengthChecker.score(null));
        assertEquals(0, StrengthChecker.score(""));
        assertEquals(0, StrengthChecker.score("abc"));          // none
        assertEquals(1, StrengthChecker.score("abcdefgh"));     // length >= 8 only
        assertEquals(2, StrengthChecker.score("abcdefghijkl")); // length >= 12 too
        assertEquals(1, StrengthChecker.score("abc123"));       // digit
        assertEquals(1, StrengthChecker.score("Abc"));          // uppercase
        assertEquals(1, StrengthChecker.score("a#c"));          // symbol
    }
}
