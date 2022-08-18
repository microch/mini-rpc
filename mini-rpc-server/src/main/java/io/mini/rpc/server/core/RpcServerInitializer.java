package io.mini.rpc.server.core;

import io.mini.rpc.codec.*;
import io.mini.rpc.serializer.JsonSerializer;
import io.mini.rpc.serializer.Serializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Map<String, Object> handlerMap;
    private final ThreadPoolExecutor threadPoolExecutor;
    Serializer serializer;

    public RpcServerInitializer(Map<String, Object> handlerMap, ThreadPoolExecutor threadPoolExecutor) {
        this.handlerMap = handlerMap;
        this.threadPoolExecutor = threadPoolExecutor;
        serializer = new JsonSerializer();
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline cp = channel.pipeline();
        cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_TIMEOUT, TimeUnit.SECONDS));
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(serializer, RpcRequest.class));
        cp.addLast(new RpcEncoder(serializer));
        cp.addLast(new RpcServerHandler(handlerMap, threadPoolExecutor));
    }
}
