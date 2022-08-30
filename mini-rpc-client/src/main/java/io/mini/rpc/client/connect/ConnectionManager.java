package io.mini.rpc.client.connect;

import io.mini.rpc.client.handler.RpcClientHandler;
import io.mini.rpc.client.handler.RpcClientInitializer;
import io.mini.rpc.client.route.RpcLoadBalance2;
import io.mini.rpc.client.route.RpcLoadBalanceRoundRobin2;
import io.mini.rpc.protocol.RpcProtocol2;
import io.mini.rpc.protocol.ServiceInstance;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private static final long WAIT_TIMEOUT = 5000;

    private final Map<RpcProtocol2, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    private final Map<String, TreeSet<RpcProtocol2>> serviceInstances = new ConcurrentHashMap<>();

    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000));

    private volatile boolean isRunning = true;

    private static volatile ConnectionManager instance;

//    private final CopyOnWriteArraySet<RpcProtocol2> rpcProtocolSet = new CopyOnWriteArraySet<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition connected = lock.newCondition();

    private final RpcLoadBalance2 loadBalance = new RpcLoadBalanceRoundRobin2();

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

    public void updateConnectedServer(Map<String, TreeSet<RpcProtocol2>> serviceMap) {
        if (!CollectionUtils.isEmpty(serviceMap)) {
            Set<RpcProtocol2> serviceSet = serviceMap.values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            // 新的
            for (final RpcProtocol2 protocol : serviceSet) {
                if (!connectedServerNodes.containsKey(protocol)) {
                    connectServerNode(protocol);
                }
            }
            // 旧的
            for (RpcProtocol2 protocol : connectedServerNodes.keySet()) {
                if (!serviceSet.contains(protocol)) {
                    logger.info("Remove invalid service: " + protocol.toString());
                    removeAndCloseHandler(protocol);
                }
            }
            serviceInstances.clear();
            serviceInstances.putAll(serviceMap);
        } else {
            logger.error("No available service!");
            for (RpcProtocol2 protocol : connectedServerNodes.keySet()) {
                removeAndCloseHandler(protocol);
            }
        }

    }

    public void updateConnectedServer(String serviceName, TreeSet<RpcProtocol2> protocol2s) {
        if (StringUtils.isEmpty(serviceName)) {
            return;
        }
        serviceInstances.put(serviceName, protocol2s);
        Set<RpcProtocol2> connected = connectedServerNodes.keySet();
        for (RpcProtocol2 protocol : protocol2s) {
            if (!connected.contains(protocol)) {
                connectServerNode(protocol);
            }
        }
    }

    public void updateConnectedServer(ServiceInstance instance) {
        if (instance == null) {
            return;
        }
        TreeSet<RpcProtocol2> protocols = serviceInstances.getOrDefault(instance.serviceName, new TreeSet<>());
        protocols.add(instance.protocol);
        serviceInstances.put(instance.serviceName, protocols);
        Set<RpcProtocol2> connected = connectedServerNodes.keySet();
        if (!connected.contains(instance.protocol)) {
            connectServerNode(instance.protocol);
        }
    }

    public void removeByServiceInstance(ServiceInstance instance) {
        if (instance == null) {
            return;
        }
        if (serviceInstances.containsKey(instance.serviceName)) {
            Set<RpcProtocol2> protocolSet = serviceInstances.get(instance.serviceName);
            if (protocolSet != null) {
                protocolSet.remove(instance.protocol);
                if (protocolSet.isEmpty()) {
                    serviceInstances.remove(instance.serviceName);
                }
            }
        }
        closeInvalidNode();
    }

    private void closeInvalidNode() {
        Set<RpcProtocol2> serviceSet = serviceInstances.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        for (RpcProtocol2 protocol : connectedServerNodes.keySet()) {
            // 该节点下已经没有实例服务
            if (!serviceSet.contains(protocol)) {
                removeAndCloseHandler(protocol);
            }
        }
    }

    private void removeAndCloseHandler(RpcProtocol2 protocol) {
        RpcClientHandler handler = connectedServerNodes.get(protocol);
        if (handler != null) {
            handler.close();
        }
        connectedServerNodes.remove(protocol);
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void connectServerNode(RpcProtocol2 protocol) {
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

    public void removeHandler(RpcProtocol2 protocol) {
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

        RpcProtocol2 rpcProtocol = loadBalance.route(serviceKey, new ArrayList<>(serviceInstances.get(serviceKey)));
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
        Set<RpcProtocol2> rpcProtocols = connectedServerNodes.keySet();
        for (RpcProtocol2 rpcProtocol : rpcProtocols) {
            removeAndCloseHandler(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

}
