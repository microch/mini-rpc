package io.mini.rpc.codec;

import io.mini.rpc.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class RpcEncoder extends MessageToByteEncoder<RpcData> {

    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);
    private final Serializer serializer;

    public RpcEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcData in, ByteBuf out) throws Exception {
        try {
            byte[] bytes = serializer.serialize(in);
            out.writeInt(bytes.length);
            out.writeBytes(bytes);
        } catch (Exception e) {
            logger.error("Encode error: " + e);
        }
    }
}
