package com.ciphervault.model;

public abstract class Account {
    protected int id;
    private String title;

    protected Account(String title) {
        this.title = title;
    }

    /** final: subclasses cannot change how the id is read. */
    public final int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public abstract String getDetails();
}
