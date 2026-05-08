package tech.beawitch.rpc.consumer;

import lombok.Data;
import tech.beawitch.rpc.loadbalance.LoadBalancePolicyType;
import tech.beawitch.rpc.register.RegistryConfig;
import tech.beawitch.rpc.retry.RetryPolicyType;

@Data
public class ConsumerProperties {
    private Integer workThreadNum = 4;
    private Integer connectTimeoutMs = 3000;
    private Integer requestTimeoutMs = 3000;
    private Integer methodTimeoutMs = 10000;
    private Integer rpcPerSecond = 10;
    private Integer rpcPerChannel = 5;
    private RegistryConfig registryConfig = new RegistryConfig();

    private LoadBalancePolicyType loadBalancePolicyType = LoadBalancePolicyType.ROUND_ROBIN;
    private RetryPolicyType retryPolicyType = RetryPolicyType.RETRY_SAME;
}
