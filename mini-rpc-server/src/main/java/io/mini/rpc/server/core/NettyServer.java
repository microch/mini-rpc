package io.mini.rpc.server.core;

import io.mini.rpc.registry.ServiceRegistry;
import io.mini.rpc.utils.ServiceUtil;
import io.mini.rpc.utils.ThreadPoolUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class NettyServer implements Server {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private Thread thread;

    private final String serverAddress;
    private final ServiceRegistry serviceRegistry;

    private final Map<String, Object> serviceMap = new HashMap<>();

    public NettyServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void start() throws Exception {
        thread = new Thread() {
            final ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.makeServerThreadPool(
                    NettyServer.class.getSimpleName(), 16, 32);

            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childOption(ChannelOption.SO_KEEPALIVE, true)
                            .childHandler(new RpcServerInitializer(serviceMap, threadPoolExecutor));

                    String[] arr = serverAddress.split(":");
                    String host = arr[0];
                    int port = Integer.parseInt(arr[1]);
                    ChannelFuture future = bootstrap.bind(host, port).sync();
                    // TODO: 2022/8/17 服务注册
                    serviceRegistry.registerService(host, port, serviceMap.keySet());
                    logger.info("Server started on port {}", port);
                    future.channel().closeFuture().sync();
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        logger.info("Rpc server remoting server stop");
                    } else {
                        logger.error("Rpc server remoting server error", e);
                    }
                } finally {
                    try {
                        // TODO: 2022/8/17 服务注销
                        serviceRegistry.unregisterService();
                        bossGroup.shutdownGracefully();
                        workerGroup.shutdownGracefully();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }

            }
        };
        thread.start();
    }

    public void addService(String interfaceName, String version, Object serviceBean) {
        logger.info("Adding service, interface: {}, version: {}, bean：{}", interfaceName, version, serviceBean);
        String serviceKey = ServiceUtil.makeServiceKey(interfaceName, version);
        serviceMap.put(serviceKey, serviceBean);
    }

    @Override
    public void stop() throws Exception {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
}
