package io.mini.rpc.server.registry.zookeeper;

import io.mini.rpc.protocol.RpcProtocol;
import io.mini.rpc.protocol.RpcServiceInfo;
import io.mini.rpc.server.registry.ServiceRegistry;
import io.mini.rpc.utils.ServiceUtil;
import io.mini.rpc.zookeeper.Constant;
import io.mini.rpc.zookeeper.CuratorClient;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class ZookeeperServiceRegistry2 implements ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    private final CuratorClient curatorClient;

    private final List<String> pathList = new ArrayList<>();


    public ZookeeperServiceRegistry2(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress, 5000);
    }

    @Override
    public void registerService(String host, int port, Collection<String> keys) {
        try {
            String node = host + ":" + port;
            for (String key : keys) {
                String path = Constant.ZK_REGISTRY_PATH + "/" + key + "/" + node;
                path = curatorClient.createPathData2(path, key.getBytes(StandardCharsets.UTF_8));
                pathList.add(path);
            }
            logger.info("Register {} new service, host: {}, port: {}", keys.size(), host, port);
        } catch (Exception e) {
            logger.error("Register service fail, exception: {}", e.getMessage());
        }
        curatorClient.addConnectionStateListener((client, newState) -> {
            if (newState == ConnectionState.RECONNECTED) {
                registerService(host, port, keys);
            }
        });
    }

    @Override
    public void unregisterService() {
        logger.info("Unregister all service");
        for (String path : pathList) {
            try {
                this.curatorClient.deletePath(path);
            } catch (Exception ex) {
                logger.error("Delete service path error: " + ex.getMessage());
            }
        }
        this.curatorClient.close();
    }
}
