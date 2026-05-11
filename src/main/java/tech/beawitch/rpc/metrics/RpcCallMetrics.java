package tech.beawitch.rpc.metrics;

import lombok.Data;
import tech.beawitch.rpc.register.ServiceMetadata;

import java.lang.reflect.Method;

@Data
public class RpcCallMetrics {
    private boolean completed;
    private Throwable throwable;
    private long duration;
    private long startTime;
    private Method method;
    private Object[] params;
    private ServiceMetadata provider;

    private RpcCallMetrics() {

    }

    public static RpcCallMetrics create(Method method, Object[] params, ServiceMetadata provider) {
        RpcCallMetrics rpcCallMetrics = new RpcCallMetrics();
        rpcCallMetrics.startTime = System.currentTimeMillis();
        return rpcCallMetrics;
    }

    public void complete() {
        this.completed = true;
        this.duration = System.currentTimeMillis() - startTime;
    }

    public void completeExceptionally(Throwable throwable) {
        this.throwable = throwable;
        this.duration = System.currentTimeMillis() - startTime;
    }
}
