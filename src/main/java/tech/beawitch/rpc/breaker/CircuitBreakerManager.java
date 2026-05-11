package tech.beawitch.rpc.breaker;

import tech.beawitch.rpc.breaker.impl.ResponseTimeCircuitBreaker;
import tech.beawitch.rpc.consumer.ConsumerProperties;
import tech.beawitch.rpc.register.ServiceMetadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CircuitBreakerManager {

    private final Map<ServiceMetadata, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();
    private final ConsumerProperties consumerProperties;

    public CircuitBreakerManager(ConsumerProperties consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    public CircuitBreaker getOrCreateCircuitBreaker(ServiceMetadata serviceMetadata) {
        return circuitBreakerMap.computeIfAbsent(serviceMetadata, this::createBreaker);
    }

    private CircuitBreaker createBreaker(ServiceMetadata serviceMetadata) {
        return new ResponseTimeCircuitBreaker(
                consumerProperties.getSlowRequestRatio(),
                consumerProperties.getSlowRequestMs()
        );
    }
}
