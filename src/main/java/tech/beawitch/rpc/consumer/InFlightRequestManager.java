package tech.beawitch.rpc.consumer;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.exception.LimitException;
import tech.beawitch.rpc.limit.Limiter;
import tech.beawitch.rpc.limit.impl.ConcurrencyLimiter;
import tech.beawitch.rpc.limit.impl.RateLimiter;
import tech.beawitch.rpc.message.Request;
import tech.beawitch.rpc.message.Response;
import tech.beawitch.rpc.register.ServiceMetadata;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class InFlightRequestManager {

    private final Map<Integer, CompletableFuture<Response>> inFlightRequestMap;

    private final HashedWheelTimer timeoutTimer;

    private final Limiter globalLimiter;

    private final Map<ServiceMetadata, Limiter> channelLimiterMap;

    private final ConsumerProperties consumerProperties;

    public InFlightRequestManager(ConsumerProperties consumerProperties) {
        this.consumerProperties = consumerProperties;
        this.inFlightRequestMap = new ConcurrentHashMap<>();
        this.timeoutTimer = new HashedWheelTimer(1, TimeUnit.SECONDS, 64);
        this.globalLimiter = new ConcurrencyLimiter(consumerProperties.getRpcPerSecond());
        this.channelLimiterMap = new ConcurrentHashMap<>();
    }

    public CompletableFuture<Response> putInFlightRequest(
            Request request,
            long timeoutMs,
            ServiceMetadata serviceMetadata
    ) {
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        if (!globalLimiter.tryAcquire()) {
            responseFuture.completeExceptionally(new LimitException("全局限流，当前在途请求超过阈值"));
            return responseFuture;
        }
        Limiter channelLimiter = channelLimiterMap.computeIfAbsent(
                serviceMetadata,
                k -> new RateLimiter(consumerProperties.getRpcPerChannel())
        );
        if (!channelLimiter.tryAcquire()) {
            responseFuture.completeExceptionally(new LimitException("channel 限流，当前在途请求超过阈值"));
            return responseFuture;
        }
        inFlightRequestMap.put(request.getId(), responseFuture);
        Timeout timeout = timeoutTimer.newTimeout(
                t -> responseFuture.completeExceptionally(new TimeoutException()),
                timeoutMs,
                TimeUnit.MILLISECONDS
        );
        responseFuture.whenComplete((response, throwable) -> {
            inFlightRequestMap.remove(request.getId());
            globalLimiter.release();
            channelLimiter.release();
            timeout.cancel();
        });
        return responseFuture;
    }

    public boolean complete(int requestId, Response response) {
        CompletableFuture<Response> future = inFlightRequestMap.get(requestId);
        if (future == null) {
            log.warn("request ID: {}，空闲返回", requestId);
            return false;
        }
        return future.complete(response);
    }

    public boolean completeExceptionally(int requestId, Exception e) {
        CompletableFuture<Response> future = inFlightRequestMap.get(requestId);
        if (future == null) {
            log.warn("request ID: {}，空闲异常", requestId, e);
            return false;
        }
        return future.completeExceptionally(e);
    }

    public void clearChannelLimiter(ServiceMetadata serviceMetadata) {
        channelLimiterMap.remove(serviceMetadata);
    }
}
