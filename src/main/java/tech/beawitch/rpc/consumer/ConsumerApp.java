package tech.beawitch.rpc.consumer;

public class ConsumerApp {

    public static void main(String[] args) throws Exception {
        Consumer consumer = new Consumer();
        System.out.println(consumer.add(1, 2));
        System.out.println(consumer.add(11, 2));
    }
}
