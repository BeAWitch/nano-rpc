package tech.beawitch.rpc.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import tech.beawitch.rpc.api.Add;
import tech.beawitch.rpc.codec.CustomDecoder;
import tech.beawitch.rpc.codec.RequestEncoder;
import tech.beawitch.rpc.exception.RpcException;
import tech.beawitch.rpc.message.Request;
import tech.beawitch.rpc.message.Response;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Consumer implements Add {

    private final Map<Integer, CompletableFuture<?>> inflightRequestMap = new ConcurrentHashMap<>();

    private final ConnectionManager connectionManager = new ConnectionManager(createBootstrap());

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
                                        CompletableFuture requestFuture =
                                                inflightRequestMap.remove(response.getRequestId());
                                        if (response.getCode() == 200) {
                                            int result = Integer.parseInt(response.getResult().toString());
                                            requestFuture.complete(result);
                                        } else {
                                            requestFuture.completeExceptionally(new RpcException(response.getErrorMessage()));
                                        }
                                    }
                                });
                    }
                });
        return bootstrap;
    }

    @Override
    public int add(int a, int b) {
        try {
            CompletableFuture<Integer> resultFuture = new CompletableFuture<>();
            Channel channel = connectionManager.getChannel("localhost", 8080);
            if (channel == null) {
                throw new RpcException("Provider 连接失败");
            }
            Request request = new Request();
            request.setServiceName(Add.class.getName());
            request.setMethodName("add");
            request.setParams(new Object[]{a, b});
            request.setParamTypes(new Class<?>[]{int.class, int.class});
            channel.writeAndFlush(request).addListener(future -> {
                if (future.isSuccess()) {
                    inflightRequestMap.put(request.getId(), resultFuture);
                }
            });
            return resultFuture.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
