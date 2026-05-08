package tech.beawitch.rpc.retry.impl;

import tech.beawitch.rpc.exception.RpcException;
import tech.beawitch.rpc.message.Response;
import tech.beawitch.rpc.register.ServiceMetadata;
import tech.beawitch.rpc.retry.RetryContext;
import tech.beawitch.rpc.retry.RetryPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Failover implements RetryPolicy {

    @Override
    public Response retry(RetryContext retryContext) throws Exception {
        List<ServiceMetadata> serviceMetadataList = new ArrayList<>(retryContext.getServiceMetadataList());
        serviceMetadataList.remove(retryContext.getFailedService());
        if (serviceMetadataList.isEmpty()) {
            throw new RpcException("没有可用于重试的 provider");
        }
        ServiceMetadata selectedService = retryContext.getLoadBalancer().select(serviceMetadataList);
        CompletableFuture<Response> future = retryContext.doRpc(selectedService);
        return future.get(
                Math.min(retryContext.getRequestTimeoutMs(), retryContext.getMethodTimeoutMs()),
                TimeUnit.MILLISECONDS
        );
    }
}
