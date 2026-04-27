package tech.beawitch.rpc.consumer;

import tech.beawitch.rpc.api.Add;
import tech.beawitch.rpc.register.RegisterConfig;

public class ConsumerApp {

    public static void main(String[] args) throws Exception {
        RegisterConfig registerConfig = new RegisterConfig();
        registerConfig.setRegisterType("zookeeper");
        registerConfig.setConnectString("localhost:2181");
        ConsumerProxyFactory consumerProxyFactory = new ConsumerProxyFactory(registerConfig);
        Add consumerProxy = consumerProxyFactory.createConsumerProxy(Add.class);
        System.out.println(consumerProxy.add(1, 2));
        System.out.println(consumerProxy.add(11, 2));
    }
}
