package com.ciphervault.model;

public class WrongPasswordException extends VaultException {
    public WrongPasswordException(String message) {
        super(message);
    }
}
