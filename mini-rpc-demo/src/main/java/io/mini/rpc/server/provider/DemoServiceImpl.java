package io.mini.rpc.server.provider;

import io.mini.rpc.annotation.MiniRpcService;
import io.mini.rpc.api.DemoService;

/**
 * @author caohao
 * @date 2022/8/18
 */
@MiniRpcService(DemoService.class)
public class DemoServiceImpl implements DemoService {
    @Override
    public Integer add(int a, int b) {
        return a + b;
    }
}
