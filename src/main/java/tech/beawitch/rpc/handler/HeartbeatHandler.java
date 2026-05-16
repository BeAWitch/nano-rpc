package tech.beawitch.rpc.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.message.HeartbeatRequest;
import tech.beawitch.rpc.message.HeartbeatResponse;

@Slf4j
public class HeartbeatHandler extends SimpleChannelInboundHandler<Object> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        if (msg instanceof HeartbeatRequest request) {
            channelHandlerContext.writeAndFlush(new HeartbeatResponse(request.getRequestTime()));
            return;
        }
        if (msg instanceof HeartbeatResponse response) {
            long duration = System.currentTimeMillis() - response.getRequestTime();
            log.info("心跳请求响应时间: {}", duration);
            return;
        }
        channelHandlerContext.fireChannelRead(msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            IdleState state = idleStateEvent.state();
            if (state == IdleState.READER_IDLE) {
                ctx.channel().close();
            } else if (state == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(new HeartbeatRequest());
            }
        }
        ctx.fireUserEventTriggered(evt);
    }
}
