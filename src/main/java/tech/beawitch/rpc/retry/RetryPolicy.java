package tech.beawitch.rpc.retry;

import tech.beawitch.rpc.message.Response;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface RetryPolicy {

    Response retry(RetryContext retryContext) throws Exception;
}
