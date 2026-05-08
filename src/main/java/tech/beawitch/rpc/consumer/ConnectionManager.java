package tech.beawitch.rpc.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.codec.CustomDecoder;
import tech.beawitch.rpc.codec.RequestEncoder;
import tech.beawitch.rpc.message.Response;
import tech.beawitch.rpc.register.ServiceMetadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConnectionManager {

    private final Map<String, ChannelHolder> channelMap = new ConcurrentHashMap<>();

    private final Bootstrap bootstrap;

    private final InFlightRequestManager inFlightRequestManager;

    private final ConsumerProperties consumerProperties;

    public ConnectionManager(InFlightRequestManager inFlightRequestManager, ConsumerProperties consumerProperties) {
        this.bootstrap = createBootstrap(consumerProperties);
        this.inFlightRequestManager = inFlightRequestManager;
        this.consumerProperties = consumerProperties;
    }

    public Channel getChannel(ServiceMetadata serviceMetadata) {
        String host = serviceMetadata.getHost();
        int port = serviceMetadata.getPort();
        String key = host + ":" + port;
        ChannelHolder channelHolder = channelMap.computeIfAbsent(key, k -> {
            try {
                ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
                Channel channel = channelFuture.channel();
                channel.closeFuture().addListener(future -> {
                    channelMap.remove(key);
                    inFlightRequestManager.clearChannelLimiter(serviceMetadata);
                });
                return new ChannelHolder(channel);
            } catch (InterruptedException e) {
                log.info("连接失败：{}:{}", host, port, e);
                return new ChannelHolder(null);
            }
        });
        Channel channel = channelHolder.channel;
        if (channel == null || !channel.isActive()) {
            channelMap.remove(key);
            return null;
        }
        return channel;
    }

    private Bootstrap createBootstrap(ConsumerProperties consumerProperties) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(consumerProperties.getWorkThreadNum()))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, consumerProperties.getConnectTimeoutMs())
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline()
                                .addLast(new CustomDecoder())
                                .addLast(new RequestEncoder())
                                .addLast(new ConsumerHandler());
                    }
                });
        return bootstrap;
    }

    private class ConsumerHandler extends SimpleChannelInboundHandler<Response> {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response response) throws Exception {
            inFlightRequestManager.complete(response.getRequestId(), response);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("地址：{} 连接了", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.info("地址：{} 断开了", ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("发生了异常", cause);
            ctx.channel().close();
        }
    }

    private static class ChannelHolder {
        private final Channel channel;

        public ChannelHolder(Channel channel) {
            this.channel = channel;
        }
    }
}
