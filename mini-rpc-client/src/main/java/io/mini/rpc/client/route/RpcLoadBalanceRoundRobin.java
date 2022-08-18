package io.mini.rpc.client.route;

import io.mini.rpc.client.handler.RpcClientHandler;
import io.mini.rpc.protocol.RpcProtocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class RpcLoadBalanceRoundRobin implements RpcLoadBalance {

    private final AtomicInteger roundRobin = new AtomicInteger(0);

    private RpcProtocol doRoute(List<RpcProtocol> addressList) {
        int size = addressList.size();
        int index = roundRobin.getAndAdd(1) % size;
        return addressList.get(index);
    }

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        }
        throw new Exception("Can not find connection for service: " + serviceKey);
    }


}
