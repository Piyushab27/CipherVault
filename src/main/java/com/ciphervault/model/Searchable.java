package com.ciphervault.model;

public interface Searchable {
    /** Interface variable: the most results a text search returns. */
    int MAX_RESULTS = 50;

    boolean matches(String text);
}
