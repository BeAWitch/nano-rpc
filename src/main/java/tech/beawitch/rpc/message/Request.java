package tech.beawitch.rpc.message;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class Request {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);
    private int id = ID_COUNTER.getAndIncrement();

    private String serviceName;
    private String methodName;
    private Class<?>[] paramTypes;
    private Object[] params;
}
