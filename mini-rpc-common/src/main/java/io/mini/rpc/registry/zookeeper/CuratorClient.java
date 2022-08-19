package io.mini.rpc.registry.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;

import java.util.List;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class CuratorClient {
    private final CuratorFramework client;

    public CuratorClient(String connectString, String namespace, int sessionTimeout, int connectionTimeout) {
        client = CuratorFrameworkFactory.builder().namespace(namespace).connectString(connectString)
                .sessionTimeoutMs(sessionTimeout).connectionTimeoutMs(connectionTimeout)
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .build();
        client.start();
    }

    public CuratorClient(String connectString, int timeout) {
        this(connectString, Constant.ZK_NAMESPACE, timeout, timeout);
    }

    public CuratorClient(String connectString) {
        this(connectString, Constant.ZK_NAMESPACE, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
    }

    public CuratorFramework getClient() {
        return client;
    }

    public void addConnectionStateListener(ConnectionStateListener connectionStateListener) {
        client.getConnectionStateListenable().addListener(connectionStateListener);
    }

    public String createPathData(String path, byte[] data) throws Exception {
        return client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(path, data);
    }

    public String createPathData2(String path, byte[] data) throws Exception {
        return client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(path, data);
    }

    public void updatePathData(String path, byte[] data) throws Exception {
        client.setData().forPath(path, data);
    }

    public void deletePath(String path) throws Exception {
        client.delete().forPath(path);
    }

    public void watchNode(String path, Watcher watcher) throws Exception {
        client.getData().usingWatcher(watcher).forPath(path);
    }

    public byte[] getData(String path) throws Exception {
        return client.getData().forPath(path);
    }

    public List<String> getChildren(String path) throws Exception {
        return client.getChildren().forPath(path);
    }

    public void watchTreeNode(String path, TreeCacheListener listener) {
//        CuratorCache treeCache = CuratorCache.builder(client, path).build();
//        treeCache.listenable().addListener();
        TreeCache treeCache = new TreeCache(client, path);
        treeCache.getListenable().addListener(listener);
    }

    public void watchPathChildrenNode(String path, PathChildrenCacheListener listener) throws Exception {
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, true);
        //BUILD_INITIAL_CACHE 代表使用同步的方式进行缓存初始化。
        pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        pathChildrenCache.getListenable().addListener(listener);
    }

    public void close() {
        client.close();
    }
}