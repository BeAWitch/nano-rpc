package tech.beawitch.rpc.provider;

public class ProviderApp {

    public static void main(String[] args) {
        ProviderServer providerServer = new ProviderServer(8080);
        providerServer.start();
    }
}
