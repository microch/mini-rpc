package io.mini.rpc.client.consumer;

import io.mini.rpc.annotation.MiniRpcAutowired;
import io.mini.rpc.api.DemoService;
import org.springframework.stereotype.Component;

/**
 * @author caohao
 * @date 2022/8/18
 */
@Component
public class TestClient {
    @MiniRpcAutowired
    private DemoService demoService;

    public void test() {
        System.out.println(demoService.sayHello("zhangsan"));
    }

}
