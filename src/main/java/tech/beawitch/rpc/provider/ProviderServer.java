package tech.beawitch.rpc.provider;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.codec.CustomDecoder;
import tech.beawitch.rpc.codec.ResponseEncoder;
import tech.beawitch.rpc.message.Request;
import tech.beawitch.rpc.message.Response;

@Slf4j
public class ProviderServer {

    private final int port;

    private final ProviderRegistry providerRegistry;

    private NioEventLoopGroup bossEventLoopGroup;
    private NioEventLoopGroup workerEventLoopGroup;

    public ProviderServer(int port) {
        this.port = port;
        providerRegistry = new ProviderRegistry();
    }

    public <I> void register(Class<I> interfaceClass, I serviceInstance) {
        providerRegistry.register(interfaceClass, serviceInstance);
    }

    public void start() {
        bossEventLoopGroup = new NioEventLoopGroup();
        workerEventLoopGroup = new NioEventLoopGroup(4);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossEventLoopGroup, workerEventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                            nioSocketChannel.pipeline()
                                    .addLast(new CustomDecoder())
                                    .addLast(new ResponseEncoder())
                                    .addLast(new ProviderHandler());
                        }
                    });
            serverBootstrap.bind(port).sync();
        } catch (Exception e) {
            throw new RuntimeException("服务器启动异常", e);
        }
    }

    public void stop() {
        if (bossEventLoopGroup != null) {
            bossEventLoopGroup.shutdownGracefully();
        }
        if (workerEventLoopGroup != null) {
            workerEventLoopGroup.shutdownGracefully();
        }
    }

    public class ProviderHandler extends SimpleChannelInboundHandler<Request> {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Request request) {
            ProviderRegistry.Invocation<?> invocation = providerRegistry.findService(request.getServiceName());
            if (invocation == null) {
                Response failResponse = Response.fail(String.format("服务不存在: %s", request.getServiceName()));
                log.info("服务不存在: {}", request.getServiceName());
                channelHandlerContext.writeAndFlush(failResponse);
                return;
            }

            try {
                Object result = invocation.invoke(
                        request.getMethodName(),
                        request.getParamTypes(),
                        request.getParams()
                );
                log.info("成功调用服务 {} 的 {} 方法，结果为: {}", request.getServiceName(), request.getMethodName(), result);
                channelHandlerContext.writeAndFlush(Response.success(result));
            } catch (Exception e) {
                Response failResponse = Response.fail(e.getMessage());
                log.info("服务调用异常", e);
                channelHandlerContext.writeAndFlush(failResponse);
            }
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
            log.info("服务端异常", cause);
            ctx.channel().close();
        }
    }
}
