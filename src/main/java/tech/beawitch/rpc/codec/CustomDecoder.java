package tech.beawitch.rpc.codec;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import tech.beawitch.rpc.compressor.Compressor;
import tech.beawitch.rpc.compressor.CompressorManager;
import tech.beawitch.rpc.exception.RpcException;
import tech.beawitch.rpc.message.Message;
import tech.beawitch.rpc.message.Request;
import tech.beawitch.rpc.message.Response;
import tech.beawitch.rpc.serializer.Serializer;
import tech.beawitch.rpc.serializer.SerializerManager;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class CustomDecoder extends LengthFieldBasedFrameDecoder {

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final int LENGTH_FIELD_OFFSET = 0;
    private static final int LENGTH_FIELD_LENGTH = Integer.BYTES;
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = Integer.BYTES;

    public volatile SerializerManager serializerManager;
    public volatile CompressorManager compressorManager;

    public CustomDecoder() {
        super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        initIfNecessary(ctx);
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        try {
            byte[] magicNumber = new byte[Message.MAGIC_NUMBER.length];
            frame.readBytes(magicNumber);
            if (!Arrays.equals(magicNumber, Message.MAGIC_NUMBER)) {
                throw new Exception("无效的魔数");
            }

            byte messageType = frame.readByte();
            short version = frame.readShort();
            byte serializerAndCompressor = frame.readByte();
            Serializer serializer = serializerManager.getSerializer((byte) ((serializerAndCompressor & 0x80) >>> 4));
            if (serializer == null) {
                throw new RpcException("没有支持的序列化器");
            }
            Compressor compressor = compressorManager.getCompressor((byte) (serializerAndCompressor & 0x08));
            if (compressor == null) {
                throw new RpcException("没有支持的压缩器");
            }
            byte[] body = new byte[frame.readableBytes()];
            frame.readBytes(body);
            body = compressor.decompress(body);
            Message.MessageType type = Message.MessageType.ofCode(messageType);
            if (type == null) {
                throw new Exception("无效的消息类型：" + messageType);
            }
            return serializer.deserialize(body, type.getMessageClass());
        } finally {
            // ByteBuf 为 native，存在本地内存中，使用引用计数进行内存回收，需要手动释放使引用减一
            frame.release();
        }
    }

    private void initIfNecessary(ChannelHandlerContext ctx) {
        if (serializerManager == null) {
            serializerManager = ctx.channel().attr(CustomEncoder.SERIALIZER_MANAGER_KEY).get();
        }
        if (compressorManager == null) {
            compressorManager = ctx.channel().attr(CustomEncoder.COMPRESSOR_MANAGER_KEY).get();
        }
    }
}
