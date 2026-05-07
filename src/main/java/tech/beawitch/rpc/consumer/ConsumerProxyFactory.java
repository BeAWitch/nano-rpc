package tech.beawitch.rpc.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
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
import tech.beawitch.rpc.register.ServiceMetadata;
import tech.beawitch.rpc.register.ServiceRegistry;
import tech.beawitch.rpc.retry.policyImpl.Failover;
import tech.beawitch.rpc.retry.policyImpl.Forking;
import tech.beawitch.rpc.retry.RetryContext;
import tech.beawitch.rpc.retry.RetryPolicy;
import tech.beawitch.rpc.retry.policyImpl.RetrySame;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class ConsumerProxyFactory {

    private final Map<Integer, CompletableFuture<Response>> inflightRequestMap;

    private final ConnectionManager connectionManager;

    private final ServiceRegistry serviceRegistry;

    private final ConsumerProperties consumerProperties;

    private final HashedWheelTimer timeoutTimer;


    public ConsumerProxyFactory(ConsumerProperties consumerProperties) throws Exception {
        this.serviceRegistry = new DefaultServiceRegistry();
        this.serviceRegistry.init(consumerProperties.getRegistryConfig());
        this.connectionManager = new ConnectionManager(createBootstrap(consumerProperties));
        this.inflightRequestMap = new ConcurrentHashMap<>();
        this.consumerProperties = consumerProperties;
        this.timeoutTimer = new HashedWheelTimer(1, TimeUnit.SECONDS, 64);
    }

    @SuppressWarnings("unchecked")
    public <I> I createConsumerProxy(Class<I> interfaceClass) {
        return (I) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{interfaceClass},
                new ConsumerInvocationHandler(interfaceClass, createLoadBalancer(), createRetryPolicy())
        );
    }

    private RetryPolicy createRetryPolicy() {
        return switch (consumerProperties.getRetryPolicyType()) {
            case RETRY_SAME -> new RetrySame();
            case FAILOVER -> new Failover();
            case FORKING -> new Forking();
        };
    }

    private LoadBalancer createLoadBalancer() {
        return switch (consumerProperties.getLoadBalancePolicyType()) {
            case RANDOM -> new RandomLoadBalancer();
            case ROUND_ROBIN -> new RoundRobinLoadBalancer();
        };
    }

    public class ConsumerInvocationHandler implements InvocationHandler {

        private final Class<?> interfaceClass;

        private final LoadBalancer loadBalancer;

        private final RetryPolicy retryPolicy;

        public ConsumerInvocationHandler(Class<?> interfaceClass, LoadBalancer loadBalancer, RetryPolicy retryPolicy) {
            this.interfaceClass = interfaceClass;
            this.loadBalancer = loadBalancer;
            this.retryPolicy = retryPolicy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            long startTime = System.currentTimeMillis();
            List<ServiceMetadata> serviceMetadata = serviceRegistry.fetchServiceList(interfaceClass.getName());
            if (serviceMetadata == null || serviceMetadata.isEmpty()) {
                throw new RpcException(interfaceClass.getName() + "没有可用的提供者");
            }
            ServiceMetadata providerMetadata = loadBalancer.select(serviceMetadata);
            Request request = buildRequest(method, args);
            Response response;
            try {
                CompletableFuture<Response> requestFuture = callRpcAsync(request, providerMetadata);
                response = requestFuture.get(consumerProperties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                long methodRemainTime =
                        consumerProperties.getMethodTimeoutMs() - (System.currentTimeMillis() - startTime);
                if (methodRemainTime <= 0) {
                    throw new TimeoutException();
                }
                RetryContext retryContext = new RetryContext();
                retryContext.setFailedService(providerMetadata);
                retryContext.setServiceMetadataList(serviceMetadata);
                retryContext.setMethodTimeoutMs(methodRemainTime);
                retryContext.setRequestTimeoutMs(consumerProperties.getRequestTimeoutMs());
                retryContext.setLoadBalancer(loadBalancer);
                retryContext.setDoRpcFunction(provider -> callRpcAsync(buildRequest(method, args), provider));
                response = this.retryPolicy.retry(retryContext);
            }
            return processResponse(response);
        }

        private CompletableFuture<Response> callRpcAsync(Request request, ServiceMetadata providerMetadata) {
            CompletableFuture<Response> responseFuture = new CompletableFuture<>();
            Channel channel = connectionManager.getChannel(providerMetadata.getHost(), providerMetadata.getPort());
            if (channel == null) {
                responseFuture.completeExceptionally(new RpcException("Provider 连接失败"));
                return responseFuture;
            }
            inflightRequestMap.put(request.getId(), responseFuture);
            Timeout timeout = timeoutTimer.newTimeout(
                    t -> responseFuture.completeExceptionally(new TimeoutException()),
                    consumerProperties.getRequestTimeoutMs(),
                    TimeUnit.MILLISECONDS
            );
            responseFuture.whenComplete((response, throwable) -> {
                inflightRequestMap.remove(request.getId());
                timeout.cancel();
            });
            channel.writeAndFlush(request).addListener(future -> {
                log.info("发送请求: {}", request.getId());
                if (!future.isSuccess()) {
                    responseFuture.completeExceptionally(future.cause());
                }
            });
            return responseFuture;
        }

        private Object processResponse(Response response) {
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
