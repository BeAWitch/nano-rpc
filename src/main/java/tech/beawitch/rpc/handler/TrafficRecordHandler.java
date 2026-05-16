package tech.beawitch.rpc.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class TrafficRecordHandler extends ChannelDuplexHandler {

    public static final AttributeKey<TrafficRecord> TRAFFIC_RECORD_KEY = AttributeKey.valueOf("trafficRecord");

    private TrafficRecord trafficRecord;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf byteBuf) {
            trafficRecord.download.addAndGet(byteBuf.readableBytes());
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf byteBuf) {
            trafficRecord.upload.addAndGet(byteBuf.readableBytes());
        }
        ctx.write(msg, promise);
        ctx.flush();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        trafficRecord = new TrafficRecord();
        ctx.channel().eventLoop().scheduleAtFixedRate(
                () -> log.info("上行流量：{}，下行流量：{}", trafficRecord.upload.get(), trafficRecord.download.get()),
                5, 5, TimeUnit.SECONDS
        );
        ctx.channel().attr(TRAFFIC_RECORD_KEY).set(trafficRecord);
        ctx.fireChannelActive();
    }

    public static class TrafficRecord {
        AtomicLong upload = new AtomicLong();
        AtomicLong download = new AtomicLong();
    }
}
