package tech.beawitch.rpc.fallback.impl;

import tech.beawitch.rpc.exception.RpcException;
import tech.beawitch.rpc.fallback.Fallback;
import tech.beawitch.rpc.fallback.RpcFallback;
import tech.beawitch.rpc.metrics.RpcCallMetrics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockFallback implements Fallback {

    private final Map<Class<?>, Object> mockObjectCache = new ConcurrentHashMap<>();

    @Override
    public Object fallback(RpcCallMetrics rpcCallMetrics) throws Exception {
        Method method = rpcCallMetrics.getMethod();
        RpcFallback annotation = method.getDeclaringClass().getAnnotation(RpcFallback.class);
        if (annotation == null) {
            throw new RpcException("未指定降级实现，降级失败");
        }
        Class<?> methodClass = annotation.value();
        if (!method.getDeclaringClass().isAssignableFrom(methodClass)) {
            throw new RpcException("降级实现类必须是" + method.getDeclaringClass() + "的子类");
        }
        Object mockObject = mockObjectCache.computeIfAbsent(methodClass, this::createMockObject);
        return method.invoke(mockObject, rpcCallMetrics.getParams());
    }

    private Object createMockObject(Class<?> methodClass) {
        try {
            return methodClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RpcException("创建降级实现类失败", e);
        }
    }
}
