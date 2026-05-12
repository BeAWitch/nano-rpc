package tech.beawitch.rpc.api;

import tech.beawitch.rpc.fallback.RpcFallback;

@RpcFallback(ConsumerAddImpl.class)
public interface Add {

    int add(int a, int b);
}
