package io.mini.rpc.codec;

import java.io.Serializable;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class RpcResponse extends RpcData {

    private static final long serialVersionUID = 8215493329459772524L;

    private String error;
    private Object result;

    public boolean isError() {
        return error != null;
    }


    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
