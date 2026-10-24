package com.ciphervault.model;

public class AccountLockedException extends VaultException {
    public AccountLockedException(String message) {
        super(message);
    }
}
