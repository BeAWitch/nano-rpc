package tech.beawitch.rpc.provider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProviderRegistry {

    private final Map<String, Invocation<?>> serviceInstanceMap = new ConcurrentHashMap<>();

    public <I> void register(Class<I> interfaceClass, I serviceInstance) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("注册类型必须为接口");
        }
        if (serviceInstanceMap.putIfAbsent(
                interfaceClass.getName(),
                new Invocation<>(interfaceClass, serviceInstance)) != null) {
            throw new IllegalArgumentException(interfaceClass.getName() + "重复注册了");
        }
    }

    public Invocation<?> findService(String serviceName) {
        return serviceInstanceMap.get(serviceName);
    }

    public static class Invocation<I> {

        private final Class<I> interfaceClass;
        private final I serviceInstance;

        public Invocation(Class<I> interfaceClass, I serviceInstance) {
            this.interfaceClass = interfaceClass;
            this.serviceInstance = serviceInstance;
        }

        public Object invoke(String methodName, Class<?>[] paramsClass, Object[] params) throws NoSuchMethodException
                , InvocationTargetException, IllegalAccessException {
            Method method = interfaceClass.getDeclaredMethod(methodName, paramsClass);
            return method.invoke(serviceInstance, params);
        }
    }
}
