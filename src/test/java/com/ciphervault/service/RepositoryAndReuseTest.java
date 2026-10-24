package com.ciphervault.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ciphervault.model.Credential;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositoryAndReuseTest {
    @Test
    void addGetAllRemove() {
        Repository<String> repo = new Repository<>();
        repo.add("a");
        repo.add("b");
        assertEquals(List.of("a", "b"), repo.getAll());
        assertTrue(repo.remove("a"));
        assertEquals(List.of("b"), repo.getAll());
        repo.getAll().clear(); // a copy: does not affect the repository
        assertEquals(1, repo.getAll().size());
    }

    @Test
    void reusedPasswordsAreFlaggedOnBothEntries() {
        Credential a = new Credential("Instagram", "me", "Same#1234");
        Credential b = new Credential("Twitter", "me", "Same#1234");
        Credential c = new Credential("Bank", "me", "Unique#9876");
        assertEquals(2, VaultService.markReused(List.of(a, b, c)));
        assertTrue(a.isReused());
        assertTrue(b.isReused());
        assertFalse(c.isReused());
    }
}
