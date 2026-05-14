package tech.beawitch.rpc.provider;

import lombok.Data;
import tech.beawitch.rpc.register.RegistryConfig;
import tech.beawitch.rpc.serializer.Serializer;

@Data
public class ProviderProperties {
    private String host;
    private int port;
    private Integer workerThreadNum = 4;
    private int globalMaxRequest = 10;
    private int maxRequestPerConsumer = 5;
    private RegistryConfig registryConfig;

    private Serializer.SerializerAlgorithm serializerAlgorithm = Serializer.SerializerAlgorithm.JSON;
}
