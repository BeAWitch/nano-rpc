package tech.beawitch.rpc.message;

import lombok.Data;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义消息格式
 */
@Data
public class Message {

    public static final byte[] MAGIC_NUMBER = "CustomMessage".getBytes(StandardCharsets.UTF_8);

    private byte[] magicNumber;

    private byte messageType;

    private short version;

    private byte serializerAndCompressor;

    private byte[] body;

    @Getter
    public enum MessageType {
        REQUEST(1, Request.class),
        RESPONSE(2, Response.class),
        HEARTBEAT_REQUEST(3, HeartbeatRequest.class),
        HEARTBEAT_RESPONSE(4, HeartbeatResponse.class);

        private static final Map<Class<?>, MessageType> CLASS_CACHE = new HashMap<>();
        private static final Map<Byte, MessageType> CODE_CACHE = new HashMap<>();
        private final byte code;
        private final Class<?> messageClass;

        static {
            for (MessageType value : values()) {
                if (CLASS_CACHE.put(value.messageClass, value) != null) {
                    throw new IllegalArgumentException("重复的消息类: " + value.messageClass);
                }
                if (CODE_CACHE.put(value.code, value) != null) {
                    throw new IllegalArgumentException("重复的消息码: " + value.code);
                }
            }
        }

        MessageType(int code, Class<?> messageClass) {
            this.code = (byte) code;
            this.messageClass = messageClass;
        }

        public static MessageType ofClass(Class<?> clazz) {
            return CLASS_CACHE.get(clazz);
        }

        public static MessageType ofCode(byte code) {
            return CODE_CACHE.get(code);
        }
    }
}
