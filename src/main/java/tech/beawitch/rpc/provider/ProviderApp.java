package tech.beawitch.rpc.provider;

import tech.beawitch.rpc.api.Add;
import tech.beawitch.rpc.register.RegistryConfig;

public class ProviderApp {

    public static void main(String[] args) {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setRegisterType("zookeeper");
        registryConfig.setConnectString("localhost:2181");
        ProviderProperties providerProperties = new ProviderProperties();
        providerProperties.setRegistryConfig(registryConfig);
        providerProperties.setHost("localhost");
        providerProperties.setPort(8088);
        ProviderServer providerServer = new ProviderServer(providerProperties);
        providerServer.register(Add.class, new AddImpl());
        providerServer.start();
    }
}
