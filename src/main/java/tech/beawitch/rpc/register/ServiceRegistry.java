package tech.beawitch.rpc.register;

import java.util.List;

public interface ServiceRegistry {

    void init(RegistryConfig config) throws Exception;

    void registerService(ServiceMetadata metadata);

    List<ServiceMetadata> fetchServiceList(String serviceName) throws Exception;
}
