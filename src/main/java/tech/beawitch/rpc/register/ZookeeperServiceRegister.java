package tech.beawitch.rpc.register;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ZookeeperServiceRegister implements ServiceRegister {

    private static final String BASE_PATH = "/rpc";

    private CuratorFramework client;

    private ServiceDiscovery<ServiceMetadata> discovery;

    @Override
    public void init(RegisterConfig config) throws Exception {
        this.client = CuratorFrameworkFactory.builder()
                .connectString(config.getConnectString())
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();

        this.discovery = ServiceDiscoveryBuilder.builder(ServiceMetadata.class)
                .basePath(BASE_PATH)
                .client(client)
                .serializer(new JsonInstanceSerializer<>(ServiceMetadata.class))
                .build();
        discovery.start();
    }

    @Override
    public void registerService(ServiceMetadata metadata) {
        try {
            ServiceInstance<ServiceMetadata> instance = ServiceInstance.<ServiceMetadata>builder()
                    .name(metadata.getServiceName())
                    .address(metadata.getHost())
                    .port(metadata.getPort())
                    .payload(metadata)
                    .build();
            discovery.registerService(instance);
        } catch (Exception e) {
            log.error("{} 注册失败", metadata, e);
            throw new RuntimeException(metadata + " 注册失败");
        }
    }

    @Override
    public List<ServiceMetadata> fetchServiceList(String serviceName) throws Exception {
        return discovery.queryForInstances(serviceName).stream().map(ServiceInstance::getPayload).toList();
    }
}
