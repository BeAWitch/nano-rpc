package tech.beawitch.rpc.provider;

import tech.beawitch.rpc.api.Add;

public class AddImpl implements Add {

    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
