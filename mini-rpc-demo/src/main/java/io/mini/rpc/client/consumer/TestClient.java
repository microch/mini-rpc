package io.mini.rpc.client.consumer;

import io.mini.rpc.annotation.MiniRpcAutowired;
import io.mini.rpc.api.DemoService;
import io.mini.rpc.api.HelloService;
import org.springframework.stereotype.Component;

/**
 * @author caohao
 * @date 2022/8/18
 */
@Component
public class TestClient {
    @MiniRpcAutowired
    private HelloService helloService;
    @MiniRpcAutowired
    private DemoService demoService;

    public void test() {
        System.out.println(helloService.sayHello("zhangsan"));
        System.out.println(demoService.add(1, 2));
    }

}
