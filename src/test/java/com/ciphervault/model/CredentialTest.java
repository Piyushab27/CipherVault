package com.ciphervault.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CredentialTest {
    @Test
    void sameSiteAndUsernameAreEqual() {
        Credential a = new Credential("Instagram", "poojitha_07", "x");
        Credential b = new Credential("instagram", "poojitha_07", "different");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new Credential("Instagram", "other", "x"));
    }

    @Test
    void searchIsCaseInsensitive() {
        Credential c = new Credential("Instagram", "poojitha_07", "x", "Social");
        assertTrue(c.matches("insta"));
        assertTrue(c.matches("INSTA"));
        assertTrue(c.matches(""));
        assertFalse(c.matches("netflix"));
    }

    @Test
    void bankCredentialExtendsTheDetails() {
        Account a = new BankCredential("SBI", "pooja", "pw", "Savings");
        assertTrue(a.getDetails().startsWith("SBI (pooja) [Banking]"));
        assertTrue(a.getDetails().endsWith("account type: Savings"));
    }

    @Test
    void detailsAndStatsNeverContainThePassword() {
        Credential c = new Credential("Gmail", "me", "Secret#123", "College");
        c.setStrength("Strong");
        c.setUpdatedAt(LocalDate.now().minusDays(10));
        c.setReused(true);
        assertFalse(c.getDetails().contains("Secret#123"));
        assertFalse(c.toString().contains("Secret#123"));
        assertEquals("College,10,Strong,true,true,10", c.toCsv());
    }

    @Test
    void defaultCategoryIsOther() {
        assertEquals("Other", new Credential("a", "b", "c").getCategory());
    }
}
