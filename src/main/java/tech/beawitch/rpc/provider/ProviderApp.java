package tech.beawitch.rpc.provider;

import tech.beawitch.rpc.api.Add;

public class ProviderApp {

    public static void main(String[] args) {
        ProviderServer providerServer = new ProviderServer(8080);
        providerServer.start();
        providerServer.register(Add.class, new AddImpl());
    }
}
