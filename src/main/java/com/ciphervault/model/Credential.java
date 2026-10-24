package com.ciphervault.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Credential extends Account implements Storable {
    private static int totalEntries;

    private String username;
    private String password;
    private String category;
    private String strength;
    private LocalDate updatedAt;
    private boolean reused;

    public Credential(String siteName, String username, String password) {
        this(siteName, username, password, "Other");
    }

    public Credential(String siteName, String username, String password, String category) {
        super(siteName);
        this.username = username;
        this.password = password;
        this.category = category;
        totalEntries++;
    }

    public static int getTotalEntries() {
        return totalEntries;
    }

    public String getSiteName() {
        return getTitle();
    }

    public void setSiteName(String siteName) {
        setTitle(siteName);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isReused() {
        return reused;
    }

    public void setReused(boolean reused) {
        this.reused = reused;
    }

    /** Never includes the password. */
    @Override
    public String getDetails() {
        return getTitle() + " (" + username + ") [" + category + "] strength: " + strength;
    }

    @Override
    public boolean matches(String text) {
        String t = text == null ? "" : text.trim().toLowerCase();
        if (t.isEmpty()) {
            return true;
        }
        return getTitle().toLowerCase().contains(t)
                || username.toLowerCase().contains(t)
                || category.toLowerCase().contains(t);
    }

    @Override
    public String toCsv() {
        boolean hasSymbol = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                hasSymbol = true;
                break;
            }
        }
        LocalDate today = LocalDate.now();
        long ageDays = ChronoUnit.DAYS.between(updatedAt == null ? today : updatedAt, today);
        return category + "," + password.length() + "," + strength + "," + hasSymbol + "," + reused + "," + ageDays;
    }

    @Override
    public String toString() {
        return getDetails();
    }

    /** Two entries are the same when the site and the username match. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Credential)) {
            return false;
        }
        Credential other = (Credential) o;
        return getTitle().equalsIgnoreCase(other.getTitle()) && username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle().toLowerCase(), username);
    }
}
