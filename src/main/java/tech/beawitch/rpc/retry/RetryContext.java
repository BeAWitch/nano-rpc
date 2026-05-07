package tech.beawitch.rpc.retry;

import lombok.Data;
import tech.beawitch.rpc.loadbalance.LoadBalancer;
import tech.beawitch.rpc.message.Response;
import tech.beawitch.rpc.register.ServiceMetadata;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Data
public class RetryContext {
    private ServiceMetadata failedService;
    private List<ServiceMetadata> serviceMetadataList;
    private long methodTimeoutMs;
    private long requestTimeoutMs;
    private LoadBalancer loadBalancer;
    private Function<ServiceMetadata, CompletableFuture<Response>> doRpcFunction;

    public CompletableFuture<Response> doRpc(ServiceMetadata serviceMetadata) {
        return doRpcFunction.apply(serviceMetadata);
    }
}
