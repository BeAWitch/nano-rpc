package tech.beawitch.rpc.consumer;

import lombok.Data;
import tech.beawitch.rpc.loadbalance.LoadBalancePolicy;
import tech.beawitch.rpc.register.RegistryConfig;

@Data
public class ConsumerProperties {
    private Integer workThreadNum = 4;
    private Integer connectTimeoutMs = 3000;
    private Integer requestTimeoutMs = 3000;
    private RegistryConfig registryConfig = new RegistryConfig();

    private LoadBalancePolicy loadBalancePolicy = LoadBalancePolicy.ROUND_ROBIN;


}
