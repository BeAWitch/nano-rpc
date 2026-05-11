package tech.beawitch.rpc.breaker;

import tech.beawitch.rpc.metrics.RpcCallMetrics;

public interface CircuitBreaker {

    boolean allowRequest();

    void recordRpc(RpcCallMetrics rpcCallMetrics);

    enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
