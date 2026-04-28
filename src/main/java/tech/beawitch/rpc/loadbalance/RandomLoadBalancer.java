package tech.beawitch.rpc.loadbalance;

import tech.beawitch.rpc.register.ServiceMetadata;

import java.util.List;
import java.util.Random;

public class RandomLoadBalancer implements LoadBalancer {

    private final Random random = new Random();

    @Override
    public ServiceMetadata select(List<ServiceMetadata> serviceList) {
        return serviceList.get(random.nextInt(0, serviceList.size()));
    }
}
