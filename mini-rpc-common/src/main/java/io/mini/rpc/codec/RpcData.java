package io.mini.rpc.codec;

import java.io.Serializable;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class RpcData implements Serializable {
    private String requestId;
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

}
