package tech.beawitch.rpc.provider;

import lombok.Data;
import tech.beawitch.rpc.register.RegistryConfig;

@Data
public class ProviderProperties {
    private String host;
    private int port;
    private Integer workerThreadNum = 4;
    private int globalMaxRequest = 10;
    private int maxRequestPerConsumer = 5;
    private RegistryConfig registryConfig;
}
