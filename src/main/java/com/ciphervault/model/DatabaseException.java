package com.ciphervault.model;

/** A VaultException caused by the database, so callers can tell it from bad user input. */
public class DatabaseException extends VaultException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
