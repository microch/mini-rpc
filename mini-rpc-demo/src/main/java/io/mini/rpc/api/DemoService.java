package io.mini.rpc.api;

import java.util.concurrent.CompletableFuture;

/**
 * @author caohao
 * @date 2022/8/18
 */
public interface DemoService {
    String sayHello(String name);

    String hahaha(String name);

    default CompletableFuture<String> sayHelloAsync(String name) {
        return CompletableFuture.completedFuture(sayHello(name));
    }
}
