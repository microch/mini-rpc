package io.mini.rpc.client.route;

import io.mini.rpc.protocol.RpcProtocol2;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author caohao
 * @date 2022/8/18
 */
public interface RpcLoadBalance2 {
    RpcProtocol2 route(String serviceKey, List<RpcProtocol2> rpcProtocols) throws Exception;

}
