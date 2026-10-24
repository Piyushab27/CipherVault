package com.ciphervault.model;

public class User {
    private final int id;
    private final String username;
    private final String masterHash;
    private final int failedAttempts;
    private final String securityQuestion;
    private final String answerHash;
    private final int resetAttempts;

    public User(int id, String username, String masterHash, int failedAttempts,
                String securityQuestion, String answerHash, int resetAttempts) {
        this.id = id;
        this.username = username;
        this.masterHash = masterHash;
        this.failedAttempts = failedAttempts;
        this.securityQuestion = securityQuestion;
        this.answerHash = answerHash;
        this.resetAttempts = resetAttempts;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getMasterHash() {
        return masterHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    /** Null when the user has not set one yet. */
    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public String getAnswerHash() {
        return answerHash;
    }

    public int getResetAttempts() {
        return resetAttempts;
    }
}
