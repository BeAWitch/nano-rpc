package tech.beawitch.rpc.register;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultServiceRegistry implements ServiceRegistry {

    private ServiceRegistry delegate;

    private final Map<String, List<ServiceMetadata>> cache = new ConcurrentHashMap<>();

    @Override
    public void init(RegistryConfig config) throws Exception {
        this.delegate = create(config);
        delegate.init(config);
    }

    @Override
    public void registerService(ServiceMetadata metadata) {
        log.info("向 {} 注册服务 {}", delegate.getClass(), metadata);
        delegate.registerService(metadata);
    }

    @Override
    public List<ServiceMetadata> fetchServiceList(String serviceName) {
        try {
            List<ServiceMetadata> serviceMetadata = delegate.fetchServiceList(serviceName);
            cache.put(serviceName, serviceMetadata);
            return serviceMetadata;
        } catch (Exception e) {
            log.error("注册中心 {} 获取服务 {} 的列表失败", delegate.getClass(), serviceName, e);
            return cache.getOrDefault(serviceName, new ArrayList<>());
        }
    }

    private ServiceRegistry create(RegistryConfig config) {
        if ("zookeeper".equals(config.getRegisterType())) {
            return new ZookeeperServiceRegistry();
        }
        throw new RuntimeException("不支持的注册中心类型：" + config.getRegisterType());
    }
}
