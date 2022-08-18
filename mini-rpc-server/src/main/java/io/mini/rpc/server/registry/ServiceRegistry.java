package io.mini.rpc.server.registry;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author caohao
 * @date 2022/8/17
 */
public interface ServiceRegistry {

    void registerService(String host, int port, Collection<String> keys);

    void unregisterService();

}
