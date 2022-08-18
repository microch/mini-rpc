package io.mini.rpc;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class RpcConfig {

    enum Serialization {
        JSON;
    }

    enum Registry {
        ZOOKEEPER;
    }

    private Serialization serialization;

    private Registry registry;


}
