package tech.beawitch.rpc.message;

import lombok.Data;

@Data
public class Response {
    private Object result;
    private int code;
    private String errorMessage;

    private final static int SUCCESS_CODE = 200;
    private final static int FAIL_CODE = 400;

    public static Response fail(String errorMessage) {
        Response response = new Response();
        response.setErrorMessage(errorMessage);
        response.setCode(FAIL_CODE);
        return response;
    }

    public static Response success(Object result) {
        Response response = new Response();
        response.setResult(result);
        response.setCode(SUCCESS_CODE);
        return response;
    }
}
