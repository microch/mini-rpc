package io.mini.rpc.client.handler;

import io.mini.rpc.codec.Beat;
import io.mini.rpc.codec.RpcDecoder;
import io.mini.rpc.codec.RpcEncoder;
import io.mini.rpc.codec.RpcResponse;
import io.mini.rpc.serializer.JsonSerializer;
import io.mini.rpc.serializer.Serializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    Serializer serializer;

    public RpcClientInitializer() {
        serializer = new JsonSerializer();
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline cp = socketChannel.pipeline();
        cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_INTERVAL, TimeUnit.SECONDS));
        cp.addLast(new RpcEncoder(serializer));
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(serializer, RpcResponse.class));
        cp.addLast(new RpcClientHandler());
    }
}
