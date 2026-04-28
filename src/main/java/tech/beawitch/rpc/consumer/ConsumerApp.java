package tech.beawitch.rpc.consumer;

import tech.beawitch.rpc.api.Add;
import tech.beawitch.rpc.register.RegistryConfig;

public class ConsumerApp {

    public static void main(String[] args) throws Exception {
        ConsumerProperties consumerProperties = new ConsumerProperties();
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setRegisterType("zookeeper");
        registryConfig.setConnectString("localhost:2181");
        consumerProperties.setRegistryConfig(registryConfig);
        ConsumerProxyFactory consumerProxyFactory = new ConsumerProxyFactory(consumerProperties);
        Add consumerProxy = consumerProxyFactory.createConsumerProxy(Add.class);
        System.out.println(consumerProxy.add(1, 2));
        System.out.println(consumerProxy.add(11, 2));
        while (true) {
            Thread.sleep(1000);
            System.out.println(consumerProxy.add(11, 2));
        }
    }
}
