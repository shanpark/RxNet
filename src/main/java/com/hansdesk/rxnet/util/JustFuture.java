package com.hansdesk.rxnet.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class JustFuture implements Future<Void> {
    private boolean cancelled = false;

    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public void done() {
        running.compareAndSet(true, false);
        for (Thread thread: waiters)
            LockSupport.unpark(thread);
    }

    public void await() {
        get();
    }

    public boolean await(long millis) {
        try {
            get(millis, TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!running.get()) {
            return false;
        } else {
            cancelled = true;
            running.compareAndSet(true, false);
            return true;
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return !running.get();
    }

    @Override
    public Void get() {
        if (running.get()) {
            waiters.add(Thread.currentThread());
            LockSupport.park(this);
        }
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws TimeoutException {
        if (unit == null)
            throw new NullPointerException();

        long nanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + nanos;
        while (running.get()) {
            LockSupport.parkNanos(this, nanos);
            if (running.get()) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0)
                    throw new TimeoutException();
            }
        }
        return null;
    }
}
