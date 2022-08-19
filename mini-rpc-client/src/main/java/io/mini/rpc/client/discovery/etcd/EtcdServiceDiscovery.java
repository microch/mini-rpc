package io.mini.rpc.client.discovery.etcd;

import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import io.mini.rpc.client.connect.ConnectionManager2;
import io.mini.rpc.registry.ServiceDiscovery;
import io.mini.rpc.protocol.RpcProtocol2;
import io.mini.rpc.protocol.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.mini.rpc.registry.etcd.EtcdConstant.SERVICE_KEYS_PREFIX;
import static io.mini.rpc.protocol.ServiceInstance.SERVICE_INVALID_KEY;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class EtcdServiceDiscovery implements ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(EtcdServiceDiscovery.class);

    private final Client client;

    private final ByteSequence serviceKeysPrefix = ByteSequence.from(SERVICE_KEYS_PREFIX, StandardCharsets.UTF_8);

    public EtcdServiceDiscovery(String... registryAddress) {
        client = Client.builder()
                .endpoints(registryAddress)
                .build();
        discoveryService();
    }

    @Override
    public void discoveryService() {
        KV kvClient = client.getKVClient();
        Watch watchClient = client.getWatchClient();
        try {
            getServiceAndUpdateServer(kvClient);
            watchClient.watch(serviceKeysPrefix, WatchOption.newBuilder().isPrefix(true).build(), new Consumer<WatchResponse>() {
                @Override
                public void accept(WatchResponse watchResponse) {
                    for (WatchEvent event : watchResponse.getEvents()) {
                        if (event.getEventType() == WatchEvent.EventType.PUT) {
                            ServiceInstance now = getServiceInstanceFromKey(event.getKeyValue());
                            ConnectionManager2.getInstance().updateConnectedServer(now);
                        } else if (event.getEventType() == WatchEvent.EventType.DELETE) {
                            ServiceInstance pre = getServiceInstanceFromKey(event.getKeyValue());
                            ConnectionManager2.getInstance().removeByServiceInstance(pre);
                        }
                    }
                }
            });

        } catch (Exception e) {
            logger.error("Get node exception: " + e.getMessage());
        }

    }

    private void getServiceAndUpdateServer(KV kvClient) throws Exception {
        GetResponse response = kvClient.get(serviceKeysPrefix, GetOption.newBuilder().isPrefix(true).build()).get();
        List<KeyValue> kvs = response.getKvs();

        Map<String, TreeSet<RpcProtocol2>> dataMap = new HashMap<>();

        Map<String, List<ServiceInstance>> serviceMap = kvs.stream()
                .map(this::getServiceInstanceFromKey)
                .collect(Collectors.groupingBy(si -> si.serviceName));

        for (Map.Entry<String, List<ServiceInstance>> entry : serviceMap.entrySet()) {
            if (!SERVICE_INVALID_KEY.equals(entry.getKey())) {
                Set<RpcProtocol2> protocols = entry.getValue().stream()
                        .map(si -> si.protocol).collect(Collectors.toSet());
                dataMap.put(entry.getKey(), new TreeSet<>(protocols));
            }
        }
        updateConnectedServer(dataMap);
    }

    private void updateConnectedServer(Map<String, TreeSet<RpcProtocol2>> dataMap) {
        ConnectionManager2.getInstance().updateConnectedServer(dataMap);
    }


    public ServiceInstance getServiceInstanceFromKey(KeyValue keyValue) {
        // prefix/name/ip:prot
        String key = keyValue.getKey().toString(StandardCharsets.UTF_8);
        String serviceNameWithHost = key.substring(SERVICE_KEYS_PREFIX.length());
        String[] arr = serviceNameWithHost.split("/");
        if (arr.length == 2 && arr[0].length() > 0) {

            return new ServiceInstance(arr[0], new RpcProtocol2(arr[1]));
        } else {
            return new ServiceInstance(SERVICE_INVALID_KEY);
        }

    }

    @Override
    public void stop() {
        client.close();
    }


}
