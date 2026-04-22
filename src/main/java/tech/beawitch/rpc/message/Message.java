package tech.beawitch.rpc.message;

import java.nio.charset.StandardCharsets;

/**
 * 自定义消息格式
 */
public class Message {
    public static final byte[] MAGIC_NUMBER = "CustomMessage".getBytes(StandardCharsets.UTF_8);

    private byte[] magicNumber;

    private byte messageType;

    private byte[] body;

    public enum MessageType {
        REQUEST(1),
        RESPONSE(2);

        private final byte code;

        MessageType(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }

        public static MessageType valueOf(byte code) {
            for (MessageType value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            return null;
        }
    }
}
