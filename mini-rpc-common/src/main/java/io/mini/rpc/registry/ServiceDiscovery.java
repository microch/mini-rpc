package io.mini.rpc.registry;

/**
 * @author caohao
 * @date 2022/8/18
 */
public interface ServiceDiscovery {
    void discoveryService();

    void stop();
}
