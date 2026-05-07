package tech.beawitch.rpc.provider;

import tech.beawitch.rpc.api.Add;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class AddImpl implements Add {

    @Override
    public int add(int a, int b) {
        /*Random random = new Random();
        if (random.nextBoolean()) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(4));
        }*/
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(4));
        return a + b;
    }
}
