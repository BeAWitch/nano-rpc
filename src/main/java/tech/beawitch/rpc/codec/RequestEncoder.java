package tech.beawitch.rpc.codec;

import com.alibaba.fastjson2.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import tech.beawitch.rpc.message.Message;
import tech.beawitch.rpc.message.Request;

import java.nio.charset.StandardCharsets;

public class RequestEncoder extends MessageToByteEncoder<Request> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Request request, ByteBuf byteBuf) throws Exception {
        byte[] magicNumber = Message.MAGIC_NUMBER;
        byte messageType = Message.MessageType.REQUEST.getCode();
        byte[] body = serializeRequest(request);
        int length = magicNumber.length + Byte.BYTES + body.length;

        byteBuf.writeInt(length);
        byteBuf.writeBytes(magicNumber);
        byteBuf.writeByte(messageType);
        byteBuf.writeBytes(body);
    }

    private byte[] serializeRequest(Request request) {
        return JSONObject.toJSONString(request).getBytes(StandardCharsets.UTF_8);
    }
}
