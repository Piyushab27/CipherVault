package com.ciphervault.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ciphervault.dao.DBConnection;
import com.ciphervault.model.AccountLockedException;
import com.ciphervault.model.Credential;
import com.ciphervault.model.WeakPasswordException;
import com.ciphervault.model.WrongPasswordException;
import com.ciphervault.model.VaultException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Integration tests (TC_IT_*) against a real MySQL. Run with MySQL up and:
 *   CIPHERVAULT_IT=1 CV_DB_USER=root CV_DB_PASS=... mvn test -Dtest=VaultServiceIT
 */
@EnabledIfEnvironmentVariable(named = "CIPHERVAULT_IT", matches = "1")
class VaultServiceIT {
    @BeforeAll
    static void setUpDatabase() throws Exception {
        DBConnection.initialise();
    }

    private static String newUser() {
        return "it_" + System.nanoTime();
    }

    private static char[] pw(String s) {
        return s.toCharArray();
    }

    @Test
    void registerThenLogInAndDuplicateUsernameIsRejected() throws Exception {
        String user = newUser();
        VaultService s = new VaultService();
        s.register(user, pw("Kavya@2026"));
        assertThrows(VaultException.class, () -> s.register(user, pw("Test@123")));
        assertThrows(WeakPasswordException.class, () -> s.register(newUser(), pw("abc123")));
        s.login(user, pw("Kavya@2026"));
        assertTrue(s.isLoggedIn());
    }

    @Test
    void forgotPasswordWithSecurityAnswer() throws Exception {
        String user = newUser();
        VaultService s = new VaultService();
        String q = VaultService.SECURITY_QUESTIONS[0];
        assertThrows(VaultException.class, () -> s.register(newUser(), pw("Vault@2026"), q, "ab"));
        s.register(user, pw("Vault@2026"), q, "Tommy");
        assertEquals(q, s.getSecurityQuestion(user));

        assertThrows(WrongPasswordException.class, () -> s.resetMasterPassword(user, "rex", pw("Fresh@2027")));
        assertThrows(WeakPasswordException.class, () -> s.resetMasterPassword(user, " tommy ", pw("abc123")));
        s.resetMasterPassword(user, " tommy ", pw("Fresh@2027")); // answer is trimmed and case-insensitive

        VaultService again = new VaultService();
        assertThrows(WrongPasswordException.class, () -> again.login(user, pw("Vault@2026")));
        again.login(user, pw("Fresh@2027"));
        assertTrue(again.isLoggedIn());
    }

    @Test
    void threeWrongAnswersLockTheReset() throws Exception {
        String user = newUser();
        VaultService s = new VaultService();
        s.register(user, pw("Vault@2026"), VaultService.SECURITY_QUESTIONS[1], "Rao");
        assertThrows(WrongPasswordException.class, () -> s.resetMasterPassword(user, "x1", pw("Fresh@2027")));
        assertThrows(WrongPasswordException.class, () -> s.resetMasterPassword(user, "x2", pw("Fresh@2027")));
        assertThrows(AccountLockedException.class, () -> s.resetMasterPassword(user, "x3", pw("Fresh@2027")));
        assertThrows(AccountLockedException.class, () -> s.resetMasterPassword(user, "Rao", pw("Fresh@2027")));
    }

    @Test
    void existingAccountCanSetAQuestionLater() throws Exception {
        String user = newUser();
        VaultService s = new VaultService();
        s.register(user, pw("Vault@2026")); // no question yet
        assertEquals(null, s.getSecurityQuestion(user));
        assertThrows(VaultException.class, () -> s.resetMasterPassword(user, "anything", pw("Fresh@2027")));
        s.login(user, pw("Vault@2026"));
        assertThrows(WrongPasswordException.class,
                () -> s.setSecurityQuestion(pw("Wrong@2026"), VaultService.SECURITY_QUESTIONS[2], "Pune"));
        s.setSecurityQuestion(pw("Vault@2026"), VaultService.SECURITY_QUESTIONS[2], "Pune");
        assertEquals(VaultService.SECURITY_QUESTIONS[2], s.getSecurityQuestion(user));
    }

    @Test
    void changeMasterPassword() throws Exception {
        String user = newUser();
        VaultService s = new VaultService();
        s.register(user, pw("Vault@2026"));
        s.login(user, pw("Vault@2026"));
        assertThrows(WrongPasswordException.class, () -> s.changeMasterPassword(pw("Nope@2026"), pw("Newpass@2027")));
        assertThrows(WeakPasswordException.class, () -> s.changeMasterPassword(pw("Vault@2026"), pw("abc123")));
        s.changeMasterPassword(pw("Vault@2026"), pw("Newpass@2027"));

        VaultService again = new VaultService();
        assertThrows(WrongPasswordException.class, () -> again.login(user, pw("Vault@2026")));
        again.login(user, pw("Newpass@2027"));
        assertTrue(again.isLoggedIn());
    }

    @Test
    void threeWrongPasswordsLockTheAccountEvenAfterARestart() throws Exception {
        String user = newUser();
        new VaultService().register(user, pw("Vault@2026"));
        VaultService s = new VaultService();
        assertThrows(WrongPasswordException.class, () -> s.login(user, pw("abc123")));
        assertThrows(WrongPasswordException.class, () -> s.login(user, pw("abc123")));
        assertThrows(AccountLockedException.class, () -> s.login(user, pw("abc123")));
        VaultService restarted = new VaultService();
        assertThrows(AccountLockedException.class, () -> restarted.login(user, pw("Vault@2026")));
    }

    @Test
    void addEditDeleteAndPersistence() throws Exception {
        String user = newUser();
        VaultService s = new VaultService();
        s.register(user, pw("Vault@2026"));
        s.login(user, pw("Vault@2026"));

        Credential c = s.addEntry("Instagram", "poojitha_07", "Insta#2026x", "Social");
        assertEquals("Strong", c.getStrength());
        assertThrows(VaultException.class, () -> s.addEntry("instagram", "poojitha_07", "x", "Social"));

        VaultService again = new VaultService();
        again.login(user, pw("Vault@2026"));
        assertEquals(1, again.getEntries().size());
        assertEquals("Insta#2026x", again.getEntries().get(0).getPassword());

        Credential loaded = again.getEntries().get(0);
        again.updateEntry(loaded, "Instagram", "poojitha_07", "Gm@il2026!", "Social");
        assertEquals(1, again.search("INSTA", "All", null).size());
        again.deleteEntry(loaded);
        assertEquals(0, again.getEntries().size());
    }
}
