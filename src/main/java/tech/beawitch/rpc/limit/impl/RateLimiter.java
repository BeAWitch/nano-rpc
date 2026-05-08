package tech.beawitch.rpc.limit.impl;

import tech.beawitch.rpc.limit.Limiter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter implements Limiter {

    private static final int MAX_TRY_ACQUIRE_COUNT = 512;
    private static final long MAX_WAIT_NS = TimeUnit.MILLISECONDS.toNanos(500);

    private final AtomicLong nextPermissionNs;
    private final long intervalNs;

    public RateLimiter(int permissionsPerSecond) {
        this.intervalNs = TimeUnit.SECONDS.toNanos(1) / permissionsPerSecond;
        this.nextPermissionNs = new AtomicLong(0L);
    }

    @Override
    public boolean tryAcquire() {
        long now = System.nanoTime();
        for (int i = 0; i < MAX_TRY_ACQUIRE_COUNT; i++) {
            long next = nextPermissionNs.get();
            if (now + MAX_WAIT_NS < next) {
                return false;
            }
            if (nextPermissionNs.compareAndSet(next, Math.max(now, next) + intervalNs)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void release(int permissions) {

    }
}
