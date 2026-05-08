package tech.beawitch.rpc.limit;

public interface Limiter {

    boolean tryAcquire();

    default void release() {
        release(1);
    }

    void release(int permissions);
}
