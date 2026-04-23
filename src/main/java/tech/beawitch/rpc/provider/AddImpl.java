package tech.beawitch.rpc.provider;

import tech.beawitch.rpc.api.Add;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class AddImpl implements Add {

    @Override
    public int add(int a, int b) {
        //LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(4));
        return a + b;
    }
}
