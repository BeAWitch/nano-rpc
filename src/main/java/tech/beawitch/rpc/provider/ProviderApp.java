package tech.beawitch.rpc.provider;

import tech.beawitch.rpc.api.Add;
import tech.beawitch.rpc.register.RegisterConfig;

public class ProviderApp {

    public static void main(String[] args) {
        RegisterConfig registerConfig = new RegisterConfig();
        registerConfig.setRegisterType("zookeeper");
        registerConfig.setConnectString("localhost:2181");
        ProviderServer providerServer = new ProviderServer("localhost", 8088, registerConfig);
        providerServer.register(Add.class, new AddImpl());
        providerServer.start();
    }
}
