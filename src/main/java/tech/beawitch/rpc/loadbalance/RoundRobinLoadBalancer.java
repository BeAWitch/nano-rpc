package tech.beawitch.rpc.loadbalance;

import tech.beawitch.rpc.register.ServiceMetadata;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger index = new AtomicInteger();

    @Override
    public ServiceMetadata select(List<ServiceMetadata> serviceList) {
        int metadataIndex = index.getAndIncrement() % serviceList.size();
        return serviceList.get(metadataIndex);
    }
}
