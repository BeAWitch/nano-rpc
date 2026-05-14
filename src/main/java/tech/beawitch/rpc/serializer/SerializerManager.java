package tech.beawitch.rpc.serializer;

import tech.beawitch.rpc.serializer.impl.HessianSerializer;
import tech.beawitch.rpc.serializer.impl.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

public class SerializerManager {

    private final Map<Byte, Serializer> serializerMap = new HashMap<>();

    public SerializerManager() {
        init();
    }

    public Serializer getSerializer(byte code) {
        return serializerMap.get(code);
    }

    private void init() {
        serializerMap.put(Serializer.SerializerAlgorithm.JSON.getCode(), new JsonSerializer());
        serializerMap.put(Serializer.SerializerAlgorithm.HESSIAN.getCode(), new HessianSerializer());
    }
}
