package com.ciphervault.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Generic in-memory store. Changes are synchronized so a thread and the UI never edit the list together. */
public class Repository<T> {
    private final List<T> items = new ArrayList<>();

    public synchronized void add(T item) {
        items.add(item);
    }

    public synchronized boolean remove(T item) {
        return items.remove(item);
    }

    public synchronized void replaceAll(Collection<? extends T> newItems) {
        items.clear();
        items.addAll(newItems);
    }

    public synchronized void clear() {
        items.clear();
    }

    /** A copy, so callers can loop while another thread changes the repository. */
    public synchronized List<T> getAll() {
        return new ArrayList<>(items);
    }

    public static <T> void printAll(List<T> items) {
        for (T item : items) {
            System.out.println(item);
        }
    }
}
