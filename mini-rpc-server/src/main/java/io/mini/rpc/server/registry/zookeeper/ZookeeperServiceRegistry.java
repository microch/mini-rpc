package io.mini.rpc.server.registry.zookeeper;

import io.mini.rpc.protocol.RpcProtocol;
import io.mini.rpc.protocol.RpcServiceInfo;
import io.mini.rpc.registry.ServiceRegistry;
import io.mini.rpc.utils.ServiceUtil;
import io.mini.rpc.registry.zookeeper.Constant;
import io.mini.rpc.registry.zookeeper.CuratorClient;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class ZookeeperServiceRegistry implements ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    private final CuratorClient curatorClient;

    private final List<String> pathList = new ArrayList<>();


    public ZookeeperServiceRegistry(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress, 5000);
    }

    @Override
    public void registerService(String host, int port, Collection<String> keys) {
        List<RpcServiceInfo> serviceInfoList = new ArrayList<>();
        for (String key : keys) {
            String[] serviceInfo = key.split(ServiceUtil.SERVICE_CONCAT_TOKEN);
            if (serviceInfo.length > 0) {
                RpcServiceInfo rpcServiceInfo = new RpcServiceInfo();
                rpcServiceInfo.setServiceName(serviceInfo[0]);
                if (serviceInfo.length == 2) {
                    rpcServiceInfo.setVersion(serviceInfo[1]);
                } else {
                    rpcServiceInfo.setVersion("");
                }
                logger.info("Register new service: {} ", key);
                serviceInfoList.add(rpcServiceInfo);
            } else {
                logger.warn("Can not get service name and version: {} ", key);
            }
        }
        try {
            RpcProtocol rpcProtocol = new RpcProtocol();
            rpcProtocol.setHost(host);
            rpcProtocol.setPort(port);
            rpcProtocol.setServiceInfoList(serviceInfoList);
            String serviceData = rpcProtocol.toJson();
            byte[] bytes = serviceData.getBytes();
            String path = Constant.ZK_DATA_PATH + "-" + rpcProtocol.hashCode();
            path = this.curatorClient.createPathData(path, bytes);
            pathList.add(path);
            logger.info("Register {} new service, host: {}, port: {}", serviceInfoList.size(), host, port);
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
