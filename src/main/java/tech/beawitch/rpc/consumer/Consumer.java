package tech.beawitch.rpc.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import tech.beawitch.rpc.api.Add;
import tech.beawitch.rpc.codec.CustomDecoder;
import tech.beawitch.rpc.codec.RequestEncoder;
import tech.beawitch.rpc.message.Request;
import tech.beawitch.rpc.message.Response;

import java.util.concurrent.CompletableFuture;

public class Consumer implements Add {

    @Override
    public int add(int a, int b) {
        try {
            CompletableFuture<Integer> addResultFuture = new CompletableFuture<>();
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
                                            System.out.println(response);
                                            int result = Integer.parseInt(response.getResult().toString());
                                            addResultFuture.complete(result);
                                        }
                                    });
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect("localhost", 8080).sync();
            Request request = new Request();
            request.setServiceName(Add.class.getName());
            request.setMethodName("add");
            request.setParams(new Object[]{a, b});
            request.setParamTypes(new Class<?>[]{int.class, int.class});
            channelFuture.channel().writeAndFlush(request);
            return addResultFuture.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
