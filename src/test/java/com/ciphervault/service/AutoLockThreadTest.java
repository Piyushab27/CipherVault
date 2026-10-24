package com.ciphervault.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AutoLockThreadTest {
    @Test
    void locksAfterIdleAndRestartsAfterUnlock() throws InterruptedException {
        Semaphore locks = new Semaphore(0);
        AutoLockThread thread = new AutoLockThread(200, locks::release);
        thread.start();
        try {
            assertTrue(thread.isLocked());
            assertFalse(locks.tryAcquire(500, TimeUnit.MILLISECONDS), "must not fire while locked");

            thread.unlockAndRestart();
            assertTrue(locks.tryAcquire(3, TimeUnit.SECONDS), "should lock after the idle time");
            assertTrue(thread.isLocked());

            thread.unlockAndRestart(); // notify() wakes it again, like a second login
            assertTrue(locks.tryAcquire(3, TimeUnit.SECONDS));
        } finally {
            thread.shutdown();
        }
    }

    @Test
    void activityKeepsItUnlocked() throws InterruptedException {
        Semaphore locks = new Semaphore(0);
        AutoLockThread thread = new AutoLockThread(400, locks::release);
        thread.start();
        try {
            thread.unlockAndRestart();
            for (int i = 0; i < 6; i++) {
                Thread.sleep(150);
                thread.touch();
            }
            assertFalse(thread.isLocked());
            assertTrue(locks.tryAcquire(3, TimeUnit.SECONDS));
        } finally {
            thread.shutdown();
        }
    }

    @Test
    void passwordHiderRunsOnceAndCanBeCancelled() throws InterruptedException {
        Semaphore hidden = new Semaphore(0);
        Thread t = PasswordHider.start(0, hidden::release);
        assertTrue(hidden.tryAcquire(2, TimeUnit.SECONDS));
        t.join();

        Thread cancelled = PasswordHider.start(30, hidden::release);
        cancelled.interrupt();
        cancelled.join(2000);
        assertFalse(hidden.tryAcquire(200, TimeUnit.MILLISECONDS));
    }
}
