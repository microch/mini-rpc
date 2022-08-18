package io.mini.rpc.client.handler;

/**
 * @author caohao
 * @date 2022/8/17
 */
public interface AsyncCallback {
    void success(Object result);

    void fail(Exception e);
}
