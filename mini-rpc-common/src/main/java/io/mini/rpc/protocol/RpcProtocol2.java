package io.mini.rpc.protocol;

import io.mini.rpc.utils.JsonUtil;
import io.mini.rpc.utils.ServiceUtil;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class RpcProtocol2 implements Serializable {
    private static final long serialVersionUID = -1102180055595190700L;
    private String host;
    private int port;

    public RpcProtocol2(String name) {
        String[] array = name.split(":");
        this.host = array[0];
        this.port = Integer.parseInt(array[1]);
    }

    public RpcProtocol2(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcProtocol2 that = (RpcProtocol2) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "RpcProtocol2{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
