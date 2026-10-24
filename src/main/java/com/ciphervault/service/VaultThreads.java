package com.ciphervault.service;

/** Both background threads belong to the "VaultThreads" thread group. */
public final class VaultThreads {
    public static final ThreadGroup GROUP = new ThreadGroup("VaultThreads");

    private VaultThreads() {
    }

    /** Stops everything in the group (used when the app exits). */
    public static void interruptAll() {
        GROUP.interrupt();
    }

    /** Cancels pending PasswordHider threads but leaves the AutoLockThread running (used on logout). */
    public static void interruptHiders() {
        Thread[] threads = new Thread[GROUP.activeCount() + 8];
        int count = GROUP.enumerate(threads, false);
        for (int i = 0; i < count; i++) {
            if (!(threads[i] instanceof AutoLockThread)) {
                threads[i].interrupt();
            }
        }
    }
}
