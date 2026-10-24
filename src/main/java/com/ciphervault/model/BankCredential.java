package com.ciphervault.model;

public class BankCredential extends Credential {
    private final String accountType;

    public BankCredential(String siteName, String username, String password, String accountType) {
        super(siteName, username, password, "Banking");
        this.accountType = accountType;
    }

    public String getAccountType() {
        return accountType;
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " | account type: " + accountType;
    }
}
