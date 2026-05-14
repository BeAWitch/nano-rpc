package tech.beawitch.rpc.serializer;

import lombok.Getter;

public interface Serializer {

    byte[] serialize(Object obj);

    <T> T deserialize(byte[] bytes, Class<T> clazz);

    @Getter
    enum SerializerAlgorithm {
        JSON(0),
        HESSIAN(1);

        private final byte code;

        SerializerAlgorithm(int code) {
            this.code = (byte) code;
        }
    }
}
