package io.mini.rpc.client.route;

import io.mini.rpc.protocol.RpcProtocol2;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class RpcLoadBalanceRoundRobin2 implements RpcLoadBalance2 {

    private final AtomicInteger roundRobin = new AtomicInteger(0);

    private RpcProtocol2 doRoute(List<RpcProtocol2> addressList) {
        int size = addressList.size();
        int index = roundRobin.getAndAdd(1) % size;
        return addressList.get(index);
    }

    @Override
    public RpcProtocol2 route(String serviceKey, List<RpcProtocol2> rpcProtocols) throws Exception {
        return doRoute(rpcProtocols);
    }

}
