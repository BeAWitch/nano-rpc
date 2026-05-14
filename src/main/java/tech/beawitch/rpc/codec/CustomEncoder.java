package tech.beawitch.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.compressor.Compressor;
import tech.beawitch.rpc.compressor.CompressorManager;
import tech.beawitch.rpc.message.Message;
import tech.beawitch.rpc.serializer.Serializer;
import tech.beawitch.rpc.serializer.SerializerManager;
import tech.beawitch.rpc.version.Version;

@Slf4j
public class CustomEncoder extends MessageToByteEncoder<Object> {

    public static final AttributeKey<Byte> SERIALIZER_KEY = AttributeKey.valueOf("serializer");
    public static final AttributeKey<SerializerManager> SERIALIZER_MANAGER_KEY =
            AttributeKey.valueOf("serializerManagerKey");

    public static final AttributeKey<Byte> COMPRESSOR_KEY = AttributeKey.valueOf("compressor");
    public static final AttributeKey<CompressorManager> COMPRESSOR_MANAGER_KEY =
            AttributeKey.valueOf("compressorManagerKey");

    public static final int COMPRESS_THRESHOLD = 1024;

    private volatile Serializer defaultSerializer;
    private volatile Compressor defaultCompressor;
    private volatile byte defaultSerializerAndCompressorCode;

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object msg, ByteBuf byteBuf) throws Exception {
        initIfNecessary(channelHandlerContext);
        Message.MessageType messageType = Message.MessageType.ofClass(msg.getClass());
        if (messageType == null) {
            log.warn("{} 不支持序列化，无法发送", msg.getClass());
            return;
        }
        byte[] magicNumber = Message.MAGIC_NUMBER;
        byte messageCode = messageType.getCode();
        Version version = Version.V1;
        byte[] body = defaultSerializer.serialize(msg);
        byte sac = defaultSerializerAndCompressorCode;
        if (body.length > COMPRESS_THRESHOLD) {
            body = defaultCompressor.compress(body);
        } else {
            sac &= (byte) 0x80;
        }
        int length = magicNumber.length + Byte.BYTES * 2 + Short.BYTES + body.length;
        byteBuf.writeInt(length);
        byteBuf.writeBytes(magicNumber);
        byteBuf.writeByte(messageCode);
        byteBuf.writeShort(version.getVersionNo());
        byteBuf.writeByte(sac);
        byteBuf.writeBytes(body);
    }

    private void initIfNecessary(ChannelHandlerContext channelHandlerContext) {
        if (defaultSerializer == null) {
            defaultSerializer = getDefaultSerializer(channelHandlerContext);
        }
        if (defaultCompressor == null) {
            defaultCompressor = getDefaultCompressor(channelHandlerContext);
        }
        if (defaultSerializerAndCompressorCode == 0) {
            defaultSerializerAndCompressorCode = getDefaultSerializerAndCompressorCode(channelHandlerContext);
        }
    }

    private Serializer getDefaultSerializer(ChannelHandlerContext channelHandlerContext) {
        Byte serializerCode = channelHandlerContext.channel().attr(SERIALIZER_KEY).get();
        SerializerManager serializerManager = channelHandlerContext.channel().attr(SERIALIZER_MANAGER_KEY).get();
        return serializerManager.getSerializer(serializerCode);
    }

    private Compressor getDefaultCompressor(ChannelHandlerContext channelHandlerContext) {
        Byte compressorCode = channelHandlerContext.channel().attr(COMPRESSOR_KEY).get();
        CompressorManager compressorManager = channelHandlerContext.channel().attr(COMPRESSOR_MANAGER_KEY).get();
        return compressorManager.getCompressor(compressorCode);
    }

    private byte getDefaultSerializerAndCompressorCode(ChannelHandlerContext channelHandlerContext) {
        byte serializerCode = channelHandlerContext.channel().attr(SERIALIZER_KEY).get();
        byte compressorCode = channelHandlerContext.channel().attr(COMPRESSOR_KEY).get();
        return (byte) ((serializerCode << 4) | compressorCode);
    }
}
