package io.mini.rpc.client.discovery.etcd;

import io.etcd.jetcd.Client;
import io.mini.rpc.client.discovery.ServiceDiscovery;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class EtcdServiceDiscovery implements ServiceDiscovery {

    private final Client client;

    public EtcdServiceDiscovery(String s) {
        client = Client.builder()
                .endpoints("http://etcd0:2379")
                .build();
    }

    @Override
    public void discoveryService() {

    }

    @Override
    public void stop() {
        client.close();
    }
}
