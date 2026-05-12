package tech.beawitch.rpc.fallback.impl;

import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.fallback.Fallback;
import tech.beawitch.rpc.metrics.RpcCallMetrics;

@Slf4j
public class DefaultFallback implements Fallback {

    private final CacheFallback cacheFallback;
    private final MockFallback mockFallback;

    public DefaultFallback(CacheFallback cacheFallback, MockFallback mockFallback) {
        this.cacheFallback = cacheFallback;
        this.mockFallback = mockFallback;
    }

    @Override
    public void recordMetrics(RpcCallMetrics rpcCallMetrics) {
        cacheFallback.recordMetrics(rpcCallMetrics);
        mockFallback.recordMetrics(rpcCallMetrics);
    }

    @Override
    public Object fallback(RpcCallMetrics rpcCallMetrics) throws Exception {
        try {
            return cacheFallback.fallback(rpcCallMetrics);
        } catch (Exception e) {
            log.warn("缓存降级失败");
            return mockFallback.fallback(rpcCallMetrics);
        }
    }
}
