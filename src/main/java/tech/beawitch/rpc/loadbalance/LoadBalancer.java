package tech.beawitch.rpc.loadbalance;

import tech.beawitch.rpc.register.ServiceMetadata;

import java.util.List;

public interface LoadBalancer {

    ServiceMetadata select(List<ServiceMetadata> serviceList);
}
