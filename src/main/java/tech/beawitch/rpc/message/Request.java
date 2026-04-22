package tech.beawitch.rpc.message;

import lombok.Data;

@Data
public class Request {
    private String serviceName;
    private String methodName;
    private String[] paramTypes;
    private Object[] params;
}
