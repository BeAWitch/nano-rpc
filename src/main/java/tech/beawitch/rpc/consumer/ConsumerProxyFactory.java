package tech.beawitch.rpc.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.codec.CustomDecoder;
import tech.beawitch.rpc.codec.RequestEncoder;
import tech.beawitch.rpc.exception.RpcException;
import tech.beawitch.rpc.message.Request;
import tech.beawitch.rpc.message.Response;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ConsumerProxyFactory {

    private final Map<Integer, CompletableFuture<Response>> inflightRequestMap = new ConcurrentHashMap<>();

    private final ConnectionManager connectionManager = new ConnectionManager(createBootstrap());

    public <I> I createConsumerProxy(Class<I> interfaceClass) {
        return (I) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getDeclaringClass() == Object.class) {
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

                        try {
                            CompletableFuture<Response> responseFuture = new CompletableFuture<>();
                            Channel channel = connectionManager.getChannel("localhost", 8080);
                            if (channel == null) {
                                throw new RpcException("Provider 连接失败");
                            }
                            Request request = new Request();
                            request.setServiceName(interfaceClass.getName());
                            request.setMethodName(method.getName());
                            request.setParams(args);
                            request.setParamTypes(method.getParameterTypes());
                            channel.writeAndFlush(request).addListener(future -> {
                                if (future.isSuccess()) {
                                    inflightRequestMap.put(request.getId(), responseFuture);
                                }
                            });
                            Response response = responseFuture.get(3, TimeUnit.SECONDS);
                            if (response.getCode() == 200) {
                                return response.getResult();
                            }
                            throw new RpcException(response.getErrorMessage());
                        } catch (RpcException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    private Bootstrap createBootstrap() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline()
                                .addLast(new CustomDecoder())
                                .addLast(new RequestEncoder())
                                .addLast(new SimpleChannelInboundHandler<Response>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                                                Response response) throws Exception {
                                        CompletableFuture<Response> responseFuture =
                                                inflightRequestMap.remove(response.getRequestId());
                                        if (responseFuture == null) {
                                            log.warn("无请求结果: {}", response.getRequestId());
                                            return;
                                        }
                                        responseFuture.complete(response);
                                    }
                                });
                    }
                });
        return bootstrap;
    }
}
