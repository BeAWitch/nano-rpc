package tech.beawitch.rpc.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.codec.CustomDecoder;
import tech.beawitch.rpc.codec.RequestEncoder;
import tech.beawitch.rpc.exception.RpcException;
import tech.beawitch.rpc.loadbalance.LoadBalancer;
import tech.beawitch.rpc.loadbalance.RandomLoadBalancer;
import tech.beawitch.rpc.loadbalance.RoundRobinLoadBalancer;
import tech.beawitch.rpc.message.Request;
import tech.beawitch.rpc.message.Response;
import tech.beawitch.rpc.register.DefaultServiceRegistry;
import tech.beawitch.rpc.register.RegistryConfig;
import tech.beawitch.rpc.register.ServiceMetadata;
import tech.beawitch.rpc.register.ServiceRegistry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class ConsumerProxyFactory {

    private final Map<Integer, CompletableFuture<Response>> inflightRequestMap;

    private final ConnectionManager connectionManager;

    private final ServiceRegistry serviceRegistry;

    private final ConsumerProperties consumerProperties;


    public ConsumerProxyFactory(ConsumerProperties consumerProperties) throws Exception {
        this.serviceRegistry = new DefaultServiceRegistry();
        this.serviceRegistry.init(consumerProperties.getRegistryConfig());
        this.connectionManager = new ConnectionManager(createBootstrap(consumerProperties));
        this.inflightRequestMap = new ConcurrentHashMap<>();
        this.consumerProperties = consumerProperties;
    }

    @SuppressWarnings("unchecked")
    public <I> I createConsumerProxy(Class<I> interfaceClass) {
        return (I) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{interfaceClass},
                new ConsumerInvocationHandler(interfaceClass, createLoadBalancer())
        );
    }

    private LoadBalancer createLoadBalancer() {
        return switch (consumerProperties.getLoadBalancePolicy()) {
            case RANDOM -> new RandomLoadBalancer();
            case ROUND_ROBIN -> new RoundRobinLoadBalancer();
        };
    }

    public class ConsumerInvocationHandler implements InvocationHandler {

        private final Class<?> interfaceClass;

        private final LoadBalancer loadBalancer;

        public ConsumerInvocationHandler(Class<?> interfaceClass, LoadBalancer loadBalancer) {
            this.interfaceClass = interfaceClass;
            this.loadBalancer = loadBalancer;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }

            CompletableFuture<Response> responseFuture = new CompletableFuture<>();
            try {
                List<ServiceMetadata> serviceMetadata = serviceRegistry.fetchServiceList(interfaceClass.getName());
                if (serviceMetadata == null || serviceMetadata.isEmpty()) {
                    throw new RpcException(interfaceClass.getName() + "没有可用的提供者");
                }
                ServiceMetadata providerMetadata = loadBalancer.select(serviceMetadata);
                Channel channel = connectionManager.getChannel(providerMetadata.getHost(), providerMetadata.getPort());
                if (channel == null) {
                    throw new RpcException("Provider 连接失败");
                }
                Request request = buildRequest(method, args);
                inflightRequestMap.put(request.getId(), responseFuture);
                channel.writeAndFlush(request).addListener(future -> {
                    if (!future.isSuccess()) {
                        inflightRequestMap.remove(request.getId());
                        responseFuture.completeExceptionally(future.cause());
                    }
                });
                return processResponse(responseFuture);
            } catch (RpcException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private Object processResponse(CompletableFuture<Response> responseFuture) throws InterruptedException,
                ExecutionException, TimeoutException {
            Response response = responseFuture.get(consumerProperties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
            if (response.getCode() == 200) {
                return response.getResult();
            }
            throw new RpcException(response.getErrorMessage());
        }

        private Request buildRequest(Method method, Object[] args) {
            Request request = new Request();
            request.setServiceName(interfaceClass.getName());
            request.setMethodName(method.getName());
            request.setParams(args);
            request.setParamTypes(method.getParameterTypes());
            return request;
        }

        private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
            if ("equals".equals(method.getName())) {
                return proxy == args[0];
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(method.getName())) {
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler " + this;
            }
            throw new UnsupportedOperationException("代理对象不支持这个方法：" + method.getName());
        }
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
            CompletableFuture<Response> responseFuture =
                    inflightRequestMap.remove(response.getRequestId());
            if (responseFuture == null) {
                log.warn("无请求结果: {}", response.getRequestId());
                return;
            }
            responseFuture.complete(response);
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
