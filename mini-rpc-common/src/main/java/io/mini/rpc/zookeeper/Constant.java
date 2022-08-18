package io.mini.rpc.zookeeper;

/**
 * @author caohao
 * @date 2022/8/18
 */
public final class Constant {
    public static final int ZK_SESSION_TIMEOUT = 5000;
    public static final int ZK_CONNECTION_TIMEOUT = 5000;

    public static final String ZK_REGISTRY_PATH = "/registry";
    public static final String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";

    public static final String ZK_NAMESPACE = "netty-rpc";
}
