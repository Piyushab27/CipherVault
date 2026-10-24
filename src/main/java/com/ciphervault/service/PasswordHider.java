package com.ciphervault.service;

/** Waits, then runs the hide action. Interrupting the thread cancels it. */
public class PasswordHider implements Runnable {
    public static final int DEFAULT_SECONDS = 30;

    private final int seconds;
    private final Runnable onHide;

    public PasswordHider(int seconds, Runnable onHide) {
        this.seconds = seconds;
        this.onHide = onHide;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            return; // cancelled
        }
        onHide.run();
    }

    /** Starts a low-priority daemon thread in the VaultThreads group. */
    public static Thread start(int seconds, Runnable onHide) {
        Thread t = new Thread(VaultThreads.GROUP, new PasswordHider(seconds, onHide), "PasswordHider");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
        return t;
    }
}
