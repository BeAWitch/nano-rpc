package tech.beawitch.rpc.limit.impl;

import tech.beawitch.rpc.limit.Limiter;

import java.util.concurrent.Semaphore;

public class ConcurrencyLimiter implements Limiter {

    private final Semaphore semaphore;

    public ConcurrencyLimiter(int maxConcurrent) {
        this.semaphore = new Semaphore(maxConcurrent);
    }

    @Override
    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    @Override
    public void release(int permissions) {
        semaphore.release(permissions);
    }
}
