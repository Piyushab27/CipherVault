package com.ciphervault.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordGeneratorTest {
    @Test
    void lengthTwelveReturnsTwelveCharacters() {
        assertEquals(12, PasswordGenerator.generate(12).length());
    }

    @Test
    void withSymbolsAlwaysContainsEveryGroup() {
        for (int i = 0; i < 300; i++) {
            String p = PasswordGenerator.generate(8 + i % 25, true);
            assertTrue(p.chars().anyMatch(Character::isLowerCase), p);
            assertTrue(p.chars().anyMatch(Character::isUpperCase), p);
            assertTrue(p.chars().anyMatch(Character::isDigit), p);
            assertTrue(p.chars().anyMatch(c -> !Character.isLetterOrDigit(c)), p);
        }
    }

    @Test
    void withoutSymbolsIsLettersAndDigits() {
        for (int i = 0; i < 100; i++) {
            assertTrue(PasswordGenerator.generate(16).chars().allMatch(Character::isLetterOrDigit));
        }
    }

    /** Black-box boundary value analysis for the range 8 to 32. */
    @Test
    void boundaryValues() {
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generate(7));
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generate(33));
        assertEquals(8, PasswordGenerator.generate(8).length());
        assertEquals(32, PasswordGenerator.generate(32, true).length());
        assertEquals("Length must be 8 to 32",
                assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generate(7)).getMessage());
    }
}
