package tech.beawitch.rpc;

public class Main {

    public static void main(String[] args) throws Exception {
        Consumer consumer = new Consumer();
        System.out.println(consumer.add(1, 2));
    }
}
