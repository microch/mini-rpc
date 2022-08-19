package io.mini.rpc.server;

import io.mini.rpc.annotation.MiniRpcService;
import io.mini.rpc.registry.ServiceRegistry;
import io.mini.rpc.server.registry.etcd.EtcdServiceRegistry;
import io.mini.rpc.server.registry.zookeeper.ZookeeperServiceRegistry2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author caohao
 * @date 2022/8/18
 */
@Configuration
@ComponentScan(value = "io.mini.rpc.server.provider", useDefaultFilters = false,
        includeFilters = {@ComponentScan.Filter(value = MiniRpcService.class)})
public class ServerConfiguration {

    @Bean
    public ServiceRegistry serviceRegistry() {
//        return new ZookeeperServiceRegistry2("127.0.0.1:2181");
        return new EtcdServiceRegistry("http://127.0.0.1:2379");
    }

    @Bean
    public RpcServer rpcServer(ServiceRegistry serviceRegistry) {
        return new RpcServer("127.0.0.1:8000", serviceRegistry);
    }

}
