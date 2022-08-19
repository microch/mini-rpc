package io.mini.rpc.server.registry.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.PutResponse;
import io.mini.rpc.registry.ServiceRegistry;
import io.mini.rpc.registry.etcd.EtcdConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author caohao
 * @date 2022/8/19
 */
public class EtcdServiceRegistry implements ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(EtcdServiceRegistry.class);


    private final List<String> pathList = new ArrayList<>();
    private final Client client;

    public EtcdServiceRegistry(String... registryAddress) {
        client = Client.builder()
                .endpoints(registryAddress)
                .build();
    }

    @Override
    public void registerService(String host, int port, Collection<String> serviceNames) {
        KV kvClient = client.getKVClient();
        try {
            for (String service : serviceNames) {
                String value = host + ":" + port;
                String key = EtcdConstant.SERVICE_KEYS_PREFIX + service + "/" + value;
                CompletableFuture<PutResponse> future = kvClient.put(ByteSequence.from(key, StandardCharsets.UTF_8),
                        ByteSequence.from(value, StandardCharsets.UTF_8));
                future.get();
                pathList.add(key);
            }
            logger.info("Register {} new service, host: {}, port: {}", serviceNames.size(), host, port);
        } catch (Exception e) {
            logger.error("Register service fail, exception: {}", e.getMessage());
        }
    }

    @Override
    public void unregisterService() {
        logger.info("Unregister all service");
        for (String path : pathList) {
            try {
                this.client.getKVClient().delete(ByteSequence.from(path, StandardCharsets.UTF_8));
            } catch (Exception ex) {
                logger.error("Delete service path error: " + ex.getMessage());
            }
        }
        this.client.close();
    }
}
