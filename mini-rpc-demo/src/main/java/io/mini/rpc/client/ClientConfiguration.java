package io.mini.rpc.client;

import io.mini.rpc.client.discovery.etcd.EtcdServiceDiscovery;
import io.mini.rpc.registry.ServiceDiscovery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author caohao
 * @date 2022/8/18
 */
@Configuration
@ComponentScan("io.mini.rpc.client.consumer")
public class ClientConfiguration {

    @Bean
    public ServiceDiscovery serviceDiscovery() {
//        return new ZookeeperServiceDiscovery2("127.0.0.1:2181");
        return new EtcdServiceDiscovery("http://127.0.0.1:2379");
    }

    @Bean
    public RpcClient rpcClient(ServiceDiscovery serviceDiscovery) {
        return new RpcClient(serviceDiscovery);
    }

}
