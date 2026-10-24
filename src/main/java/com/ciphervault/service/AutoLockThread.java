package com.ciphervault.service;

/**
 * Daemon thread that locks the app after a period with no activity.
 * While the app is locked it sleeps in wait(); a successful login calls notifyAll() through
 * unlockAndRestart() and the countdown starts again.
 */
public class AutoLockThread extends Thread {
    private final long timeoutMillis;
    private final Runnable onLock;
    private final Object lock = new Object();
    private long lastActivity = System.currentTimeMillis();
    private boolean locked = true;
    private boolean running = true;

    public AutoLockThread(long timeoutMillis, Runnable onLock) {
        super(VaultThreads.GROUP, "AutoLockThread");
        this.timeoutMillis = timeoutMillis;
        this.onLock = onLock;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                boolean fire = false;
                synchronized (lock) {
                    while (locked && running) {
                        lock.wait();
                    }
                    if (!running) {
                        return;
                    }
                    long remaining = timeoutMillis - (System.currentTimeMillis() - lastActivity);
                    if (remaining <= 0) {
                        locked = true;
                        fire = true;
                    } else {
                        lock.wait(remaining);
                    }
                }
                if (fire) {
                    onLock.run();
                }
            }
        } catch (InterruptedException e) {
            // the thread group was stopped (app exit)
        }
    }

    /** Called on any mouse or key activity. */
    public void touch() {
        synchronized (lock) {
            lastActivity = System.currentTimeMillis();
        }
    }

    /** Called after a successful login. */
    public void unlockAndRestart() {
        synchronized (lock) {
            locked = false;
            lastActivity = System.currentTimeMillis();
            lock.notifyAll();
        }
    }

    /** Called on manual logout. */
    public void lockNow() {
        synchronized (lock) {
            locked = true;
            lock.notifyAll();
        }
    }

    public boolean isLocked() {
        synchronized (lock) {
            return locked;
        }
    }

    public void shutdown() {
        synchronized (lock) {
            running = false;
            lock.notifyAll();
        }
    }
}
