package tech.beawitch.rpc.codec;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import tech.beawitch.rpc.message.Message;
import tech.beawitch.rpc.message.Request;
import tech.beawitch.rpc.message.Response;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class CustomDecoder extends LengthFieldBasedFrameDecoder {
    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final int LENGTH_FIELD_OFFSET = 0;
    private static final int LENGTH_FIELD_LENGTH = Integer.BYTES;
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = Integer.BYTES;

    public CustomDecoder() {
        super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        try {
            // 读取魔数
            byte[] magicNumber = new byte[Message.MAGIC_NUMBER.length];
            frame.readBytes(magicNumber);
            if (!Arrays.equals(magicNumber, Message.MAGIC_NUMBER)) {
                throw new Exception("无效的魔数");
            }

            // 读取消息类型和消息体
            byte messageType = frame.readByte();
            byte[] body = new byte[frame.readableBytes()];
            frame.readBytes(body);
            if (Objects.equals(messageType, Message.MessageType.REQUEST.getCode())) {
                return deserializeRequest(body);
            }
            if (Objects.equals(messageType, Message.MessageType.RESPONSE.getCode())) {
                return deserializeResponse(body);
            }
            throw new Exception("无效的消息类型：" + messageType);
        } finally {
            // ByteBuf 为 native，存在本地内存中，使用引用计数进行内存回收，需要手动释放使引用减一
            frame.release();
        }
    }

    private Request deserializeRequest(byte[] body) {
        return JSONObject.parseObject(
                new String(body, StandardCharsets.UTF_8),
                Request.class,
                JSONReader.Feature.SupportClassForName
        );
    }

    private Response deserializeResponse(byte[] body) {
        return JSONObject.parseObject(
                new String(body, StandardCharsets.UTF_8),
                Response.class,
                JSONReader.Feature.SupportClassForName
        );
    }
}
