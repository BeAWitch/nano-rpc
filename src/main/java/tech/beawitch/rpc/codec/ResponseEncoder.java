package tech.beawitch.rpc.codec;

import com.alibaba.fastjson2.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import tech.beawitch.rpc.message.Message;
import tech.beawitch.rpc.message.Response;

import java.nio.charset.StandardCharsets;

public class ResponseEncoder extends MessageToByteEncoder<Response> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Response response, ByteBuf byteBuf) throws Exception {
        byte[] magicNumber = Message.MAGIC_NUMBER;
        byte messageType = Message.MessageType.RESPONSE.getCode();
        byte[] body = serializeResponse(response);
        int length = magicNumber.length + Byte.BYTES + body.length;

        byteBuf.writeInt(length);
        byteBuf.writeBytes(magicNumber);
        byteBuf.writeByte(messageType);
        byteBuf.writeBytes(body);
    }

    private byte[] serializeResponse(Response response) {
        return JSONObject.toJSONString(response).getBytes(StandardCharsets.UTF_8);
    }
}
