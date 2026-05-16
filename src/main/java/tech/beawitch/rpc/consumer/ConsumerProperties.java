package tech.beawitch.rpc.consumer;

import lombok.Data;
import tech.beawitch.rpc.compressor.Compressor;
import tech.beawitch.rpc.loadbalance.LoadBalancePolicyType;
import tech.beawitch.rpc.register.RegistryConfig;
import tech.beawitch.rpc.retry.RetryPolicyType;
import tech.beawitch.rpc.serializer.Serializer;

@Data
public class ConsumerProperties {
    private Integer workThreadNum = 4;
    private Integer connectTimeoutMs = 3000;
    private Integer requestTimeoutMs = 3000;
    private Integer methodTimeoutMs = 30000;
    private Integer rpcPerSecond = 10;
    private Integer rpcPerChannel = 50;
    private RegistryConfig registryConfig = new RegistryConfig();
    private double slowRequestRatio = 0.5;
    private long slowRequestMs = 1000;

    private LoadBalancePolicyType loadBalancePolicyType = LoadBalancePolicyType.ROUND_ROBIN;
    private RetryPolicyType retryPolicyType = RetryPolicyType.RETRY_SAME;
    private Serializer.SerializerAlgorithm serializerAlgorithm = Serializer.SerializerAlgorithm.JSON;
    private Compressor.CompressorAlgorithm compressorAlgorithm = Compressor.CompressorAlgorithm.NONE;
}
