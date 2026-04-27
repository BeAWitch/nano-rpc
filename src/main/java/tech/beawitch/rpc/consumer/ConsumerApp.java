package tech.beawitch.rpc.consumer;

import tech.beawitch.rpc.api.Add;

public class ConsumerApp {

    public static void main(String[] args) throws Exception {
        ConsumerProxyFactory consumerProxyFactory = new ConsumerProxyFactory();
        Add consumerProxy = consumerProxyFactory.createConsumerProxy(Add.class);
        System.out.println(consumerProxy.add(1, 2));
        System.out.println(consumerProxy.add(11, 2));
    }
}
