package tech.beawitch.rpc.limit.impl;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import tech.beawitch.rpc.limit.Limiter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Deprecated
public class BucketLimiter implements Limiter {

    private static final EventLoop REFILL_EVENT_LOOP = new DefaultEventLoop(r -> {
        Thread thread = new Thread(r, "refill_event_loop");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicInteger tokens;

    private final ScheduledFuture<?> refillTask;

    public BucketLimiter(int permissionsPerSecond) {
        this.tokens = new AtomicInteger(permissionsPerSecond);
        refillTask = REFILL_EVENT_LOOP.scheduleAtFixedRate(
                () -> tokens.set(permissionsPerSecond),
                1,
                1,
                TimeUnit.SECONDS
        );
    }

    @Override
    public boolean tryAcquire() {
        while (true) {
            int current = tokens.get();
            if (current <= 0) {
                return false;
            }
            if (tokens.compareAndSet(current, current - 1)) {
                return true;
            }
        }
    }

    @Override
    public void release(int permissions) {

    }

    public void shutdown() {
        refillTask.cancel(false);
    }
}
