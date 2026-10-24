package com.ciphervault.service;

import com.ciphervault.dao.CredentialDAO;
import com.ciphervault.dao.UserDAO;
import com.ciphervault.model.AccountLockedException;
import com.ciphervault.model.Credential;
import com.ciphervault.model.DatabaseException;
import com.ciphervault.model.Searchable;
import com.ciphervault.model.User;
import com.ciphervault.model.VaultException;
import com.ciphervault.model.WeakPasswordException;
import com.ciphervault.model.WrongPasswordException;

import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** The one class the Swing screens talk to. Only the DAOs run SQL. */
public class VaultService {
    public static final int MAX_ATTEMPTS = 3;
    public static final int MAX_FIELD_LENGTH = 100;
    public static final int MAX_PASSWORD_LENGTH = 128;

    private final UserDAO userDAO;
    private final CredentialDAO credentialDAO;
    private final Repository<Credential> entries = new Repository<>();
    private User currentUser;
    private Map<String, Integer> categoryIds;

    public VaultService() {
        this(new UserDAO(), new CredentialDAO());
    }

    public VaultService(UserDAO userDAO, CredentialDAO credentialDAO) {
        this.userDAO = userDAO;
        this.credentialDAO = credentialDAO;
    }

    // ---- accounts ----

    /** The questions offered when registering or setting a security question. */
    public static final String[] SECURITY_QUESTIONS = {
        "What was the name of your first pet?",
        "What is your mother's maiden name?",
        "In which city were you born?",
        "What was the name of your first school?",
        "What is the name of your favourite teacher?"
    };

    /** Registers without a security question (no password recovery until one is set). */
    public void register(String username, char[] password) throws VaultException {
        register(username, password, null, null);
    }

    /**
     * The password array is cleared before this returns. Question and answer are both given or both null.
     */
    public void register(String username, char[] password, String question, String answer) throws VaultException {
        try {
            String name = username == null ? "" : username.trim();
            if (name.isEmpty() || name.length() > 50) {
                throw new VaultException("Username must be 1 to 50 characters");
            }
            if (password == null || password.length == 0) {
                throw new VaultException("Enter a master password");
            }
            if (StrengthChecker.WEAK.equals(StrengthChecker.check(CharBuffer.wrap(password)))) {
                throw new WeakPasswordException("Master password is too weak: use 8+ characters with a digit, "
                        + "an uppercase letter and a symbol");
            }
            checkQuestion(question, answer);
            userDAO.insert(name, PasswordUtils.hashMaster(name, password), question,
                    question == null ? null : PasswordUtils.hashAnswer(name, answer));
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new VaultException("Username already taken", e);
        } catch (SQLException e) {
            throw dbError(e);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Wrong passwords are counted in the database, so a locked account stays locked after a restart.
     * The password array is cleared before this returns.
     */
    public synchronized void login(String username, char[] password) throws VaultException {
        try {
            String name = username == null ? "" : username.trim();
            User user = userDAO.findByUsername(name);
            if (user == null) {
                throw new WrongPasswordException("Wrong master password");
            }
            if (user.getFailedAttempts() >= MAX_ATTEMPTS) {
                throw new AccountLockedException("Account locked");
            }
            String hash = PasswordUtils.hashMaster(name, password);
            if (!PasswordUtils.hashesEqual(hash, user.getMasterHash())) {
                int attempts = user.getFailedAttempts() + 1;
                userDAO.updateFailedAttempts(user.getId(), attempts);
                if (attempts >= MAX_ATTEMPTS) {
                    throw new AccountLockedException("Account locked after " + MAX_ATTEMPTS + " wrong attempts");
                }
                throw new WrongPasswordException("Wrong master password (" + (MAX_ATTEMPTS - attempts)
                        + " attempt(s) left)");
            }
            if (user.getFailedAttempts() != 0) {
                userDAO.updateFailedAttempts(user.getId(), 0);
            }
            currentUser = user;
            entries.replaceAll(credentialDAO.findByUser(user.getId()));
        } catch (SQLException e) {
            throw dbError(e);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Changes the master password of the logged-in user after checking the current one.
     * Both password arrays are cleared before this returns.
     */
    public synchronized void changeMasterPassword(char[] current, char[] newPassword) throws VaultException {
        try {
            User user = requireUser();
            if (current == null || current.length == 0 || newPassword == null || newPassword.length == 0) {
                throw new VaultException("Enter the current and the new master password");
            }
            String currentHash = PasswordUtils.hashMaster(user.getUsername(), current);
            if (!PasswordUtils.hashesEqual(currentHash, user.getMasterHash())) {
                throw new WrongPasswordException("Current master password is wrong");
            }
            if (StrengthChecker.WEAK.equals(StrengthChecker.check(CharBuffer.wrap(newPassword)))) {
                throw new WeakPasswordException("New master password is too weak: use 8+ characters with a digit, "
                        + "an uppercase letter and a symbol");
            }
            String newHash = PasswordUtils.hashMaster(user.getUsername(), newPassword);
            if (PasswordUtils.hashesEqual(newHash, user.getMasterHash())) {
                throw new VaultException("The new password is the same as the current one");
            }
            userDAO.updateMasterHash(user.getId(), newHash);
            currentUser = new User(user.getId(), user.getUsername(), newHash, 0, user.getSecurityQuestion(),
                    user.getAnswerHash(), 0);
        } catch (SQLException e) {
            throw dbError(e);
        } finally {
            if (current != null) {
                Arrays.fill(current, '\0');
            }
            if (newPassword != null) {
                Arrays.fill(newPassword, '\0');
            }
        }
    }

    /** Sets or changes the security question of the logged-in user, after checking the master password. */
    public synchronized void setSecurityQuestion(char[] masterPassword, String question, String answer)
            throws VaultException {
        try {
            User user = requireUser();
            if (masterPassword == null || masterPassword.length == 0) {
                throw new VaultException("Enter your master password");
            }
            String hash = PasswordUtils.hashMaster(user.getUsername(), masterPassword);
            if (!PasswordUtils.hashesEqual(hash, user.getMasterHash())) {
                throw new WrongPasswordException("Master password is wrong");
            }
            if (question == null) {
                throw new VaultException("Choose a security question");
            }
            checkQuestion(question, answer);
            String answerHash = PasswordUtils.hashAnswer(user.getUsername(), answer);
            userDAO.updateSecurity(user.getId(), question, answerHash);
            currentUser = new User(user.getId(), user.getUsername(), user.getMasterHash(), user.getFailedAttempts(),
                    question, answerHash, 0);
        } catch (SQLException e) {
            throw dbError(e);
        } finally {
            if (masterPassword != null) {
                Arrays.fill(masterPassword, '\0');
            }
        }
    }

    /** The user's security question, or null when there is no such user or none has been set. */
    public String getSecurityQuestion(String username) throws VaultException {
        try {
            User user = userDAO.findByUsername(username == null ? "" : username.trim());
            return user == null ? null : user.getSecurityQuestion();
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /**
     * Forgot master password: a correct answer to the security question sets a new master password and unlocks
     * the account. Three wrong answers lock the reset (run sql/unlock_user.sql to clear it).
     * The new password array is cleared before this returns.
     */
    public synchronized void resetMasterPassword(String username, String answer, char[] newPassword)
            throws VaultException {
        try {
            User user = userDAO.findByUsername(username == null ? "" : username.trim());
            if (user == null || user.getSecurityQuestion() == null) {
                throw new VaultException("No security question is set for this account");
            }
            if (user.getResetAttempts() >= MAX_ATTEMPTS) {
                throw new AccountLockedException("Too many wrong answers: password reset is locked");
            }
            if (answer == null || answer.isBlank()
                    || !PasswordUtils.hashesEqual(PasswordUtils.hashAnswer(user.getUsername(), answer),
                            user.getAnswerHash())) {
                int attempts = user.getResetAttempts() + 1;
                userDAO.updateResetAttempts(user.getId(), attempts);
                if (attempts >= MAX_ATTEMPTS) {
                    throw new AccountLockedException("Too many wrong answers: password reset is locked");
                }
                throw new WrongPasswordException("Wrong answer (" + (MAX_ATTEMPTS - attempts) + " attempt(s) left)");
            }
            if (newPassword == null || newPassword.length == 0) {
                throw new VaultException("Enter a new master password");
            }
            if (StrengthChecker.WEAK.equals(StrengthChecker.check(CharBuffer.wrap(newPassword)))) {
                throw new WeakPasswordException("New master password is too weak: use 8+ characters with a digit, "
                        + "an uppercase letter and a symbol");
            }
            userDAO.updateMasterHash(user.getId(), PasswordUtils.hashMaster(user.getUsername(), newPassword));
        } catch (SQLException e) {
            throw dbError(e);
        } finally {
            if (newPassword != null) {
                Arrays.fill(newPassword, '\0');
            }
        }
    }

    private static void checkQuestion(String question, String answer) throws VaultException {
        if (question == null && (answer == null || answer.isBlank())) {
            return;
        }
        if (question == null || question.isBlank() || question.length() > 150) {
            throw new VaultException("Choose a security question");
        }
        if (answer == null || answer.trim().length() < 3) {
            throw new VaultException("The answer must be at least 3 characters");
        }
    }

    /** Drops the decrypted entries so the garbage collector can reclaim them. */
    public synchronized void logout() {
        currentUser = null;
        entries.clear();
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public synchronized String getUsername() {
        return currentUser == null ? null : currentUser.getUsername();
    }

    // ---- entries ----

    public synchronized Credential addEntry(String site, String username, String password, String category)
            throws VaultException {
        User user = requireUser();
        Credential c = buildCredential(site, username, password, category);
        if (entries.getAll().contains(c)) {
            throw new VaultException("Entry already exists");
        }
        try {
            c.setId(credentialDAO.insert(user.getId(), categoryId(c.getCategory()), c));
            c.setUpdatedAt(LocalDate.now());
        } catch (SQLException e) {
            throw dbError(e);
        }
        entries.add(c);
        return c;
    }

    public synchronized void updateEntry(Credential target, String site, String username, String password,
                                         String category) throws VaultException {
        User user = requireUser();
        Credential candidate = buildCredential(site, username, password, category);
        candidate.setId(target.getId());
        for (Credential other : entries.getAll()) {
            if (other.getId() != target.getId() && other.equals(candidate)) {
                throw new VaultException("Entry already exists");
            }
        }
        try {
            credentialDAO.update(user.getId(), categoryId(candidate.getCategory()), candidate);
        } catch (SQLException e) {
            throw dbError(e);
        }
        target.setSiteName(candidate.getSiteName());
        target.setUsername(candidate.getUsername());
        target.setPassword(candidate.getPassword());
        target.setCategory(candidate.getCategory());
        target.setStrength(candidate.getStrength());
        target.setUpdatedAt(LocalDate.now());
    }

    public synchronized void deleteEntry(Credential target) throws VaultException {
        User user = requireUser();
        try {
            credentialDAO.delete(user.getId(), target.getId());
        } catch (SQLException e) {
            throw dbError(e);
        }
        entries.remove(target);
    }

    /** Adds the entries one by one. Duplicates and invalid rows are skipped; a database failure stops the import. */
    public synchronized int importEntries(List<Credential> imported) throws VaultException {
        int added = 0;
        for (Credential c : imported) {
            try {
                addEntry(c.getSiteName(), c.getUsername(), c.getPassword(), c.getCategory());
                added++;
            } catch (DatabaseException e) {
                throw e;
            } catch (VaultException e) {
                // duplicate, unknown category or too long: skip this row
            }
        }
        return added;
    }

    /** All entries, with the reused flag up to date. */
    public synchronized List<Credential> getEntries() {
        List<Credential> all = entries.getAll();
        markReused(all);
        return all;
    }

    /** Text search (case-insensitive) plus strength and category filters. Text searches return at most 50. */
    public synchronized List<Credential> search(String text, String strength, String category) {
        boolean hasText = text != null && !text.isBlank();
        List<Credential> found = getEntries().stream()
                .filter(c -> c.matches(text))
                .filter(c -> strength == null || strength.equals("All") || strength.equals(c.getStrength()))
                .filter(c -> category == null || category.equals(c.getCategory()))
                .collect(Collectors.toList());
        return hasText && found.size() > Searchable.MAX_RESULTS ? found.subList(0, Searchable.MAX_RESULTS) : found;
    }

    public synchronized List<String> getCategories() throws VaultException {
        return new ArrayList<>(categories().keySet());
    }

    /** Entries per category, counted with a HashMap. */
    public synchronized Map<String, Integer> categoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Credential c : entries.getAll()) {
            counts.merge(c.getCategory(), 1, Integer::sum);
        }
        return counts;
    }

    public synchronized double percentStrong() {
        List<Credential> all = entries.getAll();
        if (all.isEmpty()) {
            return 0.0;
        }
        int strong = 0;
        for (Credential c : all) {
            if (StrengthChecker.STRONG.equals(c.getStrength())) {
                strong++;
            }
        }
        return (double) strong / all.size() * 100;
    }

    /** Flags every entry whose password is used by more than one entry. Returns how many were flagged. */
    public static int markReused(List<Credential> list) {
        Set<String> seen = new HashSet<>();
        Set<String> repeated = new HashSet<>();
        for (Credential c : list) {
            if (!seen.add(c.getPassword())) {
                repeated.add(c.getPassword());
            }
        }
        int flagged = 0;
        for (Credential c : list) {
            boolean reused = repeated.contains(c.getPassword());
            c.setReused(reused);
            if (reused) {
                flagged++;
            }
        }
        return flagged;
    }

    public synchronized void exportStats(File file) throws IOException {
        List<Credential> all = getEntries();
        CsvService.exportStats(file, all);
    }

    // ---- helpers ----

    private User requireUser() throws VaultException {
        if (currentUser == null) {
            throw new VaultException("You are not logged in");
        }
        return currentUser;
    }

    private Credential buildCredential(String site, String username, String password, String category)
            throws VaultException {
        String s = site == null ? "" : site.trim();
        String u = username == null ? "" : username.trim();
        if (s.isEmpty() || u.isEmpty() || password == null || password.isEmpty()) {
            throw new VaultException("Site, username and password are required");
        }
        if (s.length() > MAX_FIELD_LENGTH || u.length() > MAX_FIELD_LENGTH) {
            throw new VaultException("Site and username must be at most " + MAX_FIELD_LENGTH + " characters");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new VaultException("Password must be at most " + MAX_PASSWORD_LENGTH + " characters");
        }
        String cat = category == null || category.isBlank() ? "Other" : category;
        if (!categories().containsKey(cat)) {
            throw new VaultException("Unknown category: " + cat);
        }
        Credential c = new Credential(s, u, password, cat);
        c.setStrength(StrengthChecker.check(password));
        return c;
    }

    private Map<String, Integer> categories() throws VaultException {
        if (categoryIds == null) {
            try {
                categoryIds = credentialDAO.findCategories();
            } catch (SQLException e) {
                throw dbError(e);
            }
        }
        return categoryIds;
    }

    private int categoryId(String category) throws VaultException {
        return categories().get(category);
    }

    private static DatabaseException dbError(SQLException e) {
        String state = e.getSQLState();
        boolean connection = e instanceof SQLTransientConnectionException
                || e instanceof SQLNonTransientConnectionException
                || state != null && state.startsWith("08");
        return new DatabaseException(connection ? "Could not connect to database"
                : "Database error: " + e.getMessage(), e);
    }
}
