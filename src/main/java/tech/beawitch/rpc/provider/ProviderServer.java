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
import tech.beawitch.rpc.register.DefaultServiceRegistry;
import tech.beawitch.rpc.register.RegistryConfig;
import tech.beawitch.rpc.register.ServiceMetadata;
import tech.beawitch.rpc.register.ServiceRegistry;

@Slf4j
public class ProviderServer {

    private final ProviderProperties providerProperties;

    private final ProviderRegistry providerRegistry;

    private final ServiceRegistry serviceRegistry;

    private NioEventLoopGroup bossEventLoopGroup;
    private NioEventLoopGroup workerEventLoopGroup;

    public ProviderServer(ProviderProperties providerProperties) {
        this.providerProperties = providerProperties;
        this.providerRegistry = new ProviderRegistry();
        this.serviceRegistry = new DefaultServiceRegistry();
    }

    public <I> void register(Class<I> interfaceClass, I serviceInstance) {
        providerRegistry.register(interfaceClass, serviceInstance);
    }

    public void start() {
        bossEventLoopGroup = new NioEventLoopGroup();
        workerEventLoopGroup = new NioEventLoopGroup(providerProperties.getWorkerThreadNum());
        try {
            serviceRegistry.init(this.providerProperties.getRegistryConfig());
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
            serverBootstrap.bind(providerProperties.getHost(), providerProperties.getPort()).sync();
            providerRegistry.allServiceName().stream().map(this::buildMetadata).forEach(serviceRegistry::registerService);
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

    private ServiceMetadata buildMetadata(String serviceName) {
        ServiceMetadata serviceMetaData = new ServiceMetadata();
        serviceMetaData.setServiceName(serviceName);
        serviceMetaData.setHost(providerProperties.getHost());
        serviceMetaData.setPort(providerProperties.getPort());
        return serviceMetaData;
    }

    public class ProviderHandler extends SimpleChannelInboundHandler<Request> {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Request request) {
            ProviderRegistry.Invocation<?> invocation = providerRegistry.findService(request.getServiceName());
            if (invocation == null) {
                Response failResponse = Response.fail(request.getId(), String.format("服务不存在: %s", request.getServiceName()));
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
                channelHandlerContext.writeAndFlush(Response.success(request.getId(), result));
            } catch (Exception e) {
                Response failResponse = Response.fail(request.getId(), e.getMessage());
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
            log.error("发生了异常", cause);
            ctx.channel().close();
        }
    }
}
