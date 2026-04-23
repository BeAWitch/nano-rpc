package tech.beawitch.rpc.provider;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import tech.beawitch.rpc.codec.CustomDecoder;
import tech.beawitch.rpc.codec.ResponseEncoder;
import tech.beawitch.rpc.message.Request;
import tech.beawitch.rpc.message.Response;

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
                                    .addLast(new SimpleChannelInboundHandler<Request>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                                                    Request request) throws Exception {
                                            ProviderRegistry.Invocation<?> service =
                                                    providerRegistry.findService(request.getServiceName());
                                            Object result = service.invoke(
                                                    request.getMethodName(),
                                                    request.getParamTypes(),
                                                    request.getParams()
                                            );
                                            Response response = new Response();
                                            response.setResult(result);
                                            channelHandlerContext.writeAndFlush(response);
                                        }
                                    });
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
}
