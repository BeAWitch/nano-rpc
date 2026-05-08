package tech.beawitch.rpc.retry.impl;

import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.exception.RpcException;
import tech.beawitch.rpc.message.Response;
import tech.beawitch.rpc.retry.RetryContext;
import tech.beawitch.rpc.retry.RetryPolicy;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RetrySame implements RetryPolicy {

    private final int maxRetryCount = 3;

    private final Random random = new Random();

    @Override
    public Response retry(RetryContext retryContext) throws Exception {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < maxRetryCount; i++) {
            long delay = nextDelay(i);
            if (delay >= 1000) {
                delay = 1000;
            }
            long methodRemainTime = retryContext.getMethodTimeoutMs() - (System.currentTimeMillis() - startTime);
            if (methodRemainTime <= 0 || delay >= methodRemainTime) {
                throw new TimeoutException();
            }
            Thread.sleep(delay);
            methodRemainTime -= delay;
            try {
                CompletableFuture<Response> future = retryContext.doRpc(retryContext.getFailedService());
                return future.get(Math.min(methodRemainTime, retryContext.getRequestTimeoutMs()), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("第{}次重试失败", i + 1, e);
            }
        }
        throw new RpcException("重试失败");
    }

    /**
     * 指数回避策略
     */
    private long nextDelay(int retryCount) {
        return 100L * (1L << retryCount) + random.nextInt(0, 50);
    }
}
