package tech.beawitch.rpc.breaker.impl;

import tech.beawitch.rpc.breaker.CircuitBreaker;
import tech.beawitch.rpc.metrics.RpcCallMetrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ResponseTimeCircuitBreaker implements CircuitBreaker {

    private final long breakMs = 10000;

    private final long windowDurationMs = 10000;

    private final long slotMs = 1000;

    private final long slowRequestMs;

    private final double slowRequestRatio;

    private final int minRequestThreshold = 5;

    private final Slot[] slots = new Slot[(int) (windowDurationMs / slotMs)];

    private volatile int currentIndex = 0;

    private volatile long currentSlotTime = System.currentTimeMillis() / slotMs * slotMs; // 取整

    private volatile long breakStartMs = 0;

    private final Lock slideLock = new ReentrantLock();

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

    public ResponseTimeCircuitBreaker(double slowRequestRatio, long slowRequestMs) {
        this.slowRequestRatio = slowRequestRatio;
        this.slowRequestMs = slowRequestMs;
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new Slot();
        }
    }

    @Override
    public boolean allowRequest() {
        if (state.get() == State.CLOSED) {
            return true;
        }
        if (state.get() == State.HALF_OPEN) {
            return false;
        }
        if (System.currentTimeMillis() - breakStartMs < breakMs) {
            return false;
        }
        return state.compareAndSet(State.OPEN, State.HALF_OPEN);
    }

    @Override
    public void recordRpc(RpcCallMetrics rpcCallMetrics) {
        long now = System.currentTimeMillis();
        slideWindowIfNeeded(now);
        Slot slot = slots[currentIndex];
        slot.requestCount.incrementAndGet();
        boolean slowRequest = !rpcCallMetrics.isCompleted() || rpcCallMetrics.getDuration() > slowRequestMs;
        switch (state.get()) {
            case CLOSED -> processClosed(slowRequest);
            case OPEN -> processOpen(slowRequest);
            case HALF_OPEN -> processHalfOpen(slowRequest);
        }
    }

    private void processOpen(boolean slowRequest) {

    }

    private void processClosed(boolean slowRequest) {
        if (!slowRequest) {
            slots[currentIndex].requestCount.incrementAndGet();
            return;
        }
        slots[currentIndex].requestCount.incrementAndGet();
        slots[currentIndex].errorRequestCount.incrementAndGet();
        int totalRequestCount = 0;
        int totalErrorRequestCount = 0;
        for (Slot slot : slots) {
            totalRequestCount += slot.requestCount.get();
            totalErrorRequestCount += slot.errorRequestCount.get();
        }
        if (totalRequestCount < minRequestThreshold) {
            return;
        }
        if ((double) totalErrorRequestCount > slowRequestRatio * totalRequestCount
                && state.compareAndSet(State.CLOSED, State.OPEN)
        ) {
            breakStartMs = System.currentTimeMillis();
        }
    }

    private void processHalfOpen(boolean slowRequest) {
        if (!slowRequest) {
            state.compareAndSet(State.HALF_OPEN, State.CLOSED);
            return;
        }
        if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
            breakStartMs = System.currentTimeMillis();
        }
    }

    private void slideWindowIfNeeded(long now) {
        if (now < currentSlotTime + slotMs) {
            return;
        }
        try {
            slideLock.lock();
            int diff = (int) ((now - currentSlotTime) / slotMs);
            if (diff <= 0) {
                return;
            }
            int step = Math.min(diff, slots.length);
            for (int i = 0; i < step; i++) {
                int index = (currentIndex + i + 1) % slots.length;
                slots[index].requestCount.set(0);
                slots[index].errorRequestCount.set(0);
            }
            currentIndex = (currentIndex + step) % slots.length;
            currentSlotTime = now / slotMs * slotMs;
        } finally {
            slideLock.unlock();
        }
    }

    private static class Slot {
        AtomicInteger requestCount = new AtomicInteger(0);
        AtomicInteger errorRequestCount = new AtomicInteger(0);
    }
}
