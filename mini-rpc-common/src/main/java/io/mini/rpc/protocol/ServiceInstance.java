package io.mini.rpc.protocol;

import io.etcd.jetcd.ByteSequence;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author caohao
 * @date 2022/8/19
 */
public class ServiceInstance {
    public final static String SERVICE_INVALID_KEY = "invalid_key";
    public String serviceName;
    public RpcProtocol2 protocol;

    public ServiceInstance() {
    }

    public ServiceInstance(String serviceName) {
        this.serviceName = serviceName;
    }

    public ServiceInstance(String serviceName, String host, Integer port) {
        this.serviceName = serviceName;
        this.protocol = new RpcProtocol2(host, port);
    }

    public ServiceInstance(String serviceName, RpcProtocol2 protocol) {
        this.serviceName = serviceName;
        this.protocol = protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return Objects.equals(serviceName, that.serviceName) && Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, protocol);
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "serviceName='" + serviceName + '\'' +
                ", protocol=" + protocol +
                '}';
    }
}