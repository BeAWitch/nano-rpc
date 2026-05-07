package tech.beawitch.rpc.retry.policyImpl;

import tech.beawitch.rpc.exception.RpcException;
import tech.beawitch.rpc.message.Response;
import tech.beawitch.rpc.register.ServiceMetadata;
import tech.beawitch.rpc.retry.RetryContext;
import tech.beawitch.rpc.retry.RetryPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Forking implements RetryPolicy {


    @Override
    public Response retry(RetryContext retryContext) throws Exception {
        List<ServiceMetadata> serviceMetadataList = new ArrayList<>(retryContext.getServiceMetadataList());
        serviceMetadataList.remove(retryContext.getFailedService());
        if (serviceMetadataList.isEmpty()) {
            throw new RpcException("没有可用于重试的 provider");
        }
        CompletableFuture[] futureList = new CompletableFuture[serviceMetadataList.size()];
        for (int i = 0; i < serviceMetadataList.size(); i++) {
            CompletableFuture<Response> future = retryContext.doRpc(serviceMetadataList.get(i));
            futureList[i] = future;
        }
        CompletableFuture<Object> future = CompletableFuture.anyOf(futureList);
        return (Response) future.get(
                Math.min(retryContext.getRequestTimeoutMs(), retryContext.getMethodTimeoutMs()),
                TimeUnit.MILLISECONDS
        );
    }
}
