package tech.beawitch.rpc.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConnectionManager {

    private final Map<String, ChannelHolder> channelMap = new ConcurrentHashMap<>();

    private final Bootstrap bootstrap;

    public ConnectionManager(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public Channel getChannel(String host, int port) {
        String key = host + ":" + port;
        ChannelHolder channelHolder = channelMap.computeIfAbsent(key, k -> {
            try {
                ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
                Channel channel = channelFuture.channel();
                channel.closeFuture().addListener(future -> channelMap.remove(key));
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

    private static class ChannelHolder {
        private final Channel channel;

        public ChannelHolder(Channel channel) {
            this.channel = channel;
        }
    }
}
