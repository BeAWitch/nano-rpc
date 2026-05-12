package tech.beawitch.rpc.fallback.impl;

import lombok.Data;
import tech.beawitch.rpc.exception.RpcException;
import tech.beawitch.rpc.fallback.Fallback;
import tech.beawitch.rpc.metrics.RpcCallMetrics;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class CacheFallback implements Fallback {

    private static final Object NULL = new Object();

    private final Map<InvokeKey, Object> rpcResultCache = new ConcurrentHashMap<>();

    @Override
    public Object fallback(RpcCallMetrics rpcCallMetrics) {
        InvokeKey invokeKey = new InvokeKey(rpcCallMetrics.getMethod(), rpcCallMetrics.getParams());
        Object result = rpcResultCache.get(invokeKey);
        if (result == NULL) {
            return null;
        }
        if (result == null) {
            throw new RpcException("缓存降级失败");
        }
        return result;
    }

    @Override
    public void recordMetrics(RpcCallMetrics rpcCallMetrics) {
        InvokeKey invokeKey = new InvokeKey(rpcCallMetrics.getMethod(), rpcCallMetrics.getParams());
        Object result = rpcCallMetrics.getResult();
        if (result == null) {
            result = NULL;
        }
        rpcResultCache.put(invokeKey, result);
    }

    @Data
    private static class InvokeKey {
        final Method method;
        final Object[] args;

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            InvokeKey invokeKey = (InvokeKey) object;
            return Objects.equals(method, invokeKey.method) && Arrays.equals(args, invokeKey.args);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(method);
            result = 31 * result + Arrays.hashCode(args);
            return result;
        }
    }
}
