package com.ciphervault.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordUtilsTest {
    @Test
    void sha256KnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                PasswordUtils.sha256Hex("abc"));
    }

    @Test
    void masterHashFitsTheColumnAndDependsOnUsername() {
        String a = PasswordUtils.hashMaster("poojitha", "Vault@2026".toCharArray());
        assertEquals(64, a.length());
        assertEquals(a, PasswordUtils.hashMaster("POOJITHA", "Vault@2026".toCharArray()));
        assertNotEquals(a, PasswordUtils.hashMaster("kavya", "Vault@2026".toCharArray()));
        assertNotEquals(a, PasswordUtils.hashMaster("poojitha", "Vault@2027".toCharArray()));
        assertTrue(PasswordUtils.hashesEqual(a, a));
        assertFalse(PasswordUtils.hashesEqual(a, "x"));
    }

    @Test
    void answerHashIgnoresCaseAndSpacesButNotUsernameOrText() {
        String a = PasswordUtils.hashAnswer("poojitha", "Tommy");
        assertEquals(64, a.length());
        assertEquals(a, PasswordUtils.hashAnswer("POOJITHA", "  tommy "));
        assertNotEquals(a, PasswordUtils.hashAnswer("kavya", "Tommy"));
        assertNotEquals(a, PasswordUtils.hashAnswer("poojitha", "Rex"));
    }

    @Test
    void encodeDecodeRoundTripAndIsNotPlain() {
        String pw = "G7#kq!2Lm9@x é";
        String stored = PasswordUtils.encode(pw);
        assertNotEquals(pw, stored);
        assertEquals(pw, PasswordUtils.decode(stored));
    }
}
