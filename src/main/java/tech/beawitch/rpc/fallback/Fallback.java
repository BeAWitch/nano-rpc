package tech.beawitch.rpc.fallback;

import tech.beawitch.rpc.metrics.RpcCallMetrics;

public interface Fallback {

    Object fallback(RpcCallMetrics rpcCallMetrics) throws Exception;

    default void recordMetrics(RpcCallMetrics rpcCallMetrics) {

    }
}
