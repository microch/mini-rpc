package io.mini.rpc.client;

import io.mini.rpc.client.discovery.ServiceDiscovery;
import io.mini.rpc.client.discovery.zookeeper.ZookeeperServiceDiscovery;
import io.mini.rpc.client.discovery.zookeeper.ZookeeperServiceDiscovery2;
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
        return new ZookeeperServiceDiscovery2("127.0.0.1:2181");
    }

    @Bean
    public RpcClient rpcClient(ServiceDiscovery serviceDiscovery) {
        return new RpcClient(serviceDiscovery);
    }

}
