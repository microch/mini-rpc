package io.mini.rpc.client.discovery.zookeeper;

import io.mini.rpc.client.connect.ConnectionManager;
import io.mini.rpc.registry.ServiceDiscovery;
import io.mini.rpc.protocol.RpcProtocol;
import io.mini.rpc.registry.zookeeper.Constant;
import io.mini.rpc.registry.zookeeper.CuratorClient;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class ZookeeperServiceDiscovery implements ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperServiceDiscovery.class);

    private final CuratorClient curatorClient;

    public ZookeeperServiceDiscovery(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress);
        discoveryService();
    }

    @Override
    public void discoveryService() {
        try {
            // Get initial service info
            logger.info("Get initial service info");
            getServiceAndUpdateServer();
            // Add watch listener
            curatorClient.watchPathChildrenNode(Constant.ZK_REGISTRY_PATH, (framework, event) -> {
                PathChildrenCacheEvent.Type type = event.getType();
                ChildData childData = event.getData();
                switch (type) {
                    case CONNECTION_RECONNECTED:
                        logger.info("Reconnected to zk, try to get latest service list");
                        getServiceAndUpdateServer();
                        break;
                    case CHILD_ADDED:
                        getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_ADDED);
                        break;
                    case CHILD_UPDATED:
                        getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_UPDATED);
                        break;
                    case CHILD_REMOVED:
                        getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_REMOVED);
                        break;
                }
            });
        } catch (Exception ex) {
            logger.error("Watch node exception: " + ex.getMessage());
        }
    }

    private void getServiceAndUpdateServer() {
        try {
            List<String> nodeList = curatorClient.getChildren(Constant.ZK_REGISTRY_PATH);
            List<RpcProtocol> dataList = new ArrayList<>();
            for (String node : nodeList) {
                logger.debug("Service node: " + node);
                byte[] bytes = curatorClient.getData(Constant.ZK_REGISTRY_PATH + "/" + node);
                String json = new String(bytes);
                RpcProtocol rpcProtocol = RpcProtocol.fromJson(json);
                dataList.add(rpcProtocol);
            }
            logger.debug("Service node data: {}", dataList);
            //Update the service info based on the latest data
            updateConnectedServer(dataList);
        } catch (Exception e) {
            logger.error("Get node exception: " + e.getMessage());
        }
    }

    private void updateConnectedServer(List<RpcProtocol> dataList) {
        ConnectionManager.getInstance().updateConnectedServer(dataList);
    }


    private void getServiceAndUpdateServer(ChildData childData, PathChildrenCacheEvent.Type type) {
        String path = childData.getPath();
        String data = new String(childData.getData(), StandardCharsets.UTF_8);
        logger.info("Child data updated, path:{},type:{},data:{},", path, type, data);
        RpcProtocol rpcProtocol = RpcProtocol.fromJson(data);
        updateConnectedServer(rpcProtocol, type);
    }

    private void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        ConnectionManager.getInstance().updateConnectedServer(rpcProtocol, type);
    }

    @Override
    public void stop() {
        this.curatorClient.close();
    }
}
