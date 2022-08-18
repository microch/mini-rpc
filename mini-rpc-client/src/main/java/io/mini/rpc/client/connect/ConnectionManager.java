package io.mini.rpc.client.connect;

import io.mini.rpc.client.handler.RpcClientHandler;
import io.mini.rpc.client.handler.RpcClientInitializer;
import io.mini.rpc.client.route.RpcLoadBalance;
import io.mini.rpc.client.route.RpcLoadBalanceRoundRobin;
import io.mini.rpc.protocol.RpcProtocol;
import io.mini.rpc.protocol.RpcServiceInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private static final long WAIT_TIMEOUT = 5000;


    private final Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000));

    private volatile boolean isRunning = true;

    private static volatile ConnectionManager instance;

    private final CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition connected = lock.newCondition();

    private RpcLoadBalance loadBalance = new RpcLoadBalanceRoundRobin();

    private ConnectionManager() {
    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            synchronized (ConnectionManager.class) {
                if (instance == null) {
                    instance = new ConnectionManager();
                }
            }
        }
        return instance;
    }

    public void updateConnectedServer(List<RpcProtocol> serviceList) {
        if (!CollectionUtils.isEmpty(serviceList)) {
            HashSet<RpcProtocol> serviceSet = new HashSet<>(serviceList.size());
            serviceSet.addAll(serviceList);

            // 新的
            for (final RpcProtocol protocol : serviceSet) {
                if (!rpcProtocolSet.contains(protocol)) {
                    connectServerNode(protocol);
                }
            }
            // 旧的
            for (RpcProtocol protocol : rpcProtocolSet) {
                if (!serviceSet.contains(protocol)) {
                    logger.info("Remove invalid service: " + protocol.toJson());
                    removeAndCloseHandler(protocol);
                }
            }
        } else {
            logger.error("No available service!");
            for (RpcProtocol protocol : rpcProtocolSet) {
                removeAndCloseHandler(protocol);
            }
        }

    }

    public void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        if (rpcProtocol == null) {
            return;
        }
        if (type == PathChildrenCacheEvent.Type.CHILD_ADDED && !rpcProtocolSet.contains(rpcProtocol)) {
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
            //TODO We may don't need to reconnect remote server if the server'IP and server'port are not changed
            removeAndCloseHandler(rpcProtocol);
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            removeAndCloseHandler(rpcProtocol);
        } else {
            throw new IllegalArgumentException("Unknow type:" + type);
        }
    }

    private void removeAndCloseHandler(RpcProtocol protocol) {
        RpcClientHandler handler = connectedServerNodes.get(protocol);
        if (handler != null) {
            handler.close();
        }
        connectedServerNodes.remove(protocol);
        rpcProtocolSet.remove(protocol);
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void connectServerNode(RpcProtocol protocol) {
        if (CollectionUtils.isEmpty(protocol.getServiceInfoList())) {
            logger.info("No service on node, host: {}, port: {}", protocol.getHost(), protocol.getPort());
            return;
        }
        rpcProtocolSet.add(protocol);
        logger.info("New service node, host: {}, port: {}", protocol.getHost(), protocol.getPort());
        for (RpcServiceInfo serviceProtocol : protocol.getServiceInfoList()) {
            logger.info("New service info, name: {}, version: {}", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }

        final InetSocketAddress remotePeer = new InetSocketAddress(protocol.getHost(), protocol.getPort());
        threadPoolExecutor.submit(() -> {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer());
            ChannelFuture channelFuture = b.connect(remotePeer);
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    logger.info("Successfully connect to remote server, remote peer = " + remotePeer);
                    RpcClientHandler handler = future.channel().pipeline().get(RpcClientHandler.class);
                    connectedServerNodes.put(protocol, handler);
                    handler.setRpcProtocol(protocol);
                    signalAvailableHandler();
                } else {
                    logger.error("Can not connect to remote server, remote peer = " + remotePeer);
                }
            });
        });
    }

    public void removeHandler(RpcProtocol protocol) {
        rpcProtocolSet.remove(protocol);
        connectedServerNodes.remove(protocol);
        logger.info("Remove one connection, host: {}, port: {}", protocol.getHost(), protocol.getPort());
    }


    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        int size = connectedServerNodes.values().size();
        while (isRunning && size <= 0) {
            try {
                waitingForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                logger.error("Waiting for available service is interrupted!", e);
            }
        }
        RpcProtocol rpcProtocol = loadBalance.route(serviceKey, connectedServerNodes);
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler == null) {
            throw new Exception("Can not get available connection");
        }
        return handler;
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            logger.warn("Waiting for available service");
            return connected.await(WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        isRunning = false;
        for (RpcProtocol rpcProtocol : rpcProtocolSet) {
            removeAndCloseHandler(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

}
