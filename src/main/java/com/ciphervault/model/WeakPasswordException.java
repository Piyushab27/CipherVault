package com.ciphervault.model;

public class WeakPasswordException extends VaultException {
    public WeakPasswordException(String message) {
        super(message);
    }
}
