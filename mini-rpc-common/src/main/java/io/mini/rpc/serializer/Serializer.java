package io.mini.rpc.serializer;

/**
 * @author caohao
 * @date 2022/8/17
 */
public interface Serializer {

    <T> byte[] serialize(T obj);

    <T> T deserialize(byte[] bytes,Class<T> clazz);

}
