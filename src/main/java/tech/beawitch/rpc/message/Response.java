package tech.beawitch.rpc.message;

import lombok.Data;

@Data
public class Response {
    private Object result;
    private int code;
    private String errorMessage;
    private int requestId;

    private final static int SUCCESS_CODE = 200;
    private final static int FAIL_CODE = 400;

    public static Response fail(int requestId, String errorMessage) {
        Response response = new Response();
        response.setErrorMessage(errorMessage);
        response.setCode(FAIL_CODE);
        response.setRequestId(requestId);
        return response;
    }

    public static Response success(int requestId, Object result) {
        Response response = new Response();
        response.setResult(result);
        response.setCode(SUCCESS_CODE);
        response.setRequestId(requestId);
        return response;
    }
}
