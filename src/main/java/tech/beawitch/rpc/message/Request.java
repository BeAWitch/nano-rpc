package tech.beawitch.rpc.message;

import lombok.Data;

@Data
public class Request {
    private String serviceName;
    private String methodName;
    private Class<?>[] paramTypes;
    private Object[] params;
}
