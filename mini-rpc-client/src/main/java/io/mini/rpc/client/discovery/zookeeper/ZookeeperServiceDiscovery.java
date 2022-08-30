package io.mini.rpc.client.discovery.zookeeper;

import io.mini.rpc.client.connect.ConnectionManager;
import io.mini.rpc.registry.ServiceDiscovery;
import io.mini.rpc.protocol.RpcProtocol2;
import io.mini.rpc.registry.zookeeper.Constant;
import io.mini.rpc.registry.zookeeper.CuratorClient;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
            List<String> serviceList = curatorClient.getChildren(Constant.ZK_REGISTRY_PATH);
            Map<String, TreeSet<RpcProtocol2>> dataMap = new HashMap<>();

            for (String service : serviceList) {
                logger.debug("Service: " + service);
                List<String> instances = curatorClient.getChildren(Constant.ZK_REGISTRY_PATH + "/" + service);
                Set<RpcProtocol2> protocols = instances.stream().map(RpcProtocol2::new).collect(Collectors.toSet());
                dataMap.put(service, new TreeSet<>(protocols));
            }
            logger.debug("Service node data: {}", dataMap);
            //Update the service info based on the latest data
            updateConnectedServer(dataMap);
        } catch (Exception e) {
            logger.error("Get node exception: " + e.getMessage());
        }
    }

    private void updateConnectedServer(Map<String, TreeSet<RpcProtocol2>> dataMap) {
        ConnectionManager.getInstance().updateConnectedServer(dataMap);
    }


    private void getServiceAndUpdateServer(ChildData childData, PathChildrenCacheEvent.Type type) {
        String path = childData.getPath();
        String serviceName = new String(childData.getData(), StandardCharsets.UTF_8);
        logger.info("Child data updated, path:{},type:{},serviceName:{},", path, type, serviceName);
        if (!StringUtils.isEmpty(serviceName)) {
            try {
                List<String> instances = curatorClient.getChildren(Constant.ZK_REGISTRY_PATH + "/" + serviceName);
                Set<RpcProtocol2> protocols = instances.stream().map(RpcProtocol2::new).collect(Collectors.toSet());
                ConnectionManager.getInstance().updateConnectedServer(serviceName, new TreeSet<>(protocols));
            } catch (Exception e) {
                logger.error("Child update failed,", e);
            }
        }
    }


    @Override
    public void stop() {
        this.curatorClient.close();
    }
}
