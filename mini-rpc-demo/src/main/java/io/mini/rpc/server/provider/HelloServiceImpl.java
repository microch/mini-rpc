package io.mini.rpc.server.provider;

import io.mini.rpc.annotation.MiniRpcService;
import io.mini.rpc.api.DemoService;
import io.mini.rpc.api.HelloService;

/**
 * @author caohao
 * @date 2022/8/18
 */
@MiniRpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
