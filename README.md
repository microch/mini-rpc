# mini-rpc
RPC，即 Remote Procedure Call（远程过程调用）， 像调用本地服务一样，调用远程计算机上的服务。 RPC可以很好的解耦系统，如WebService就是一种基于Http协议的RPC。

本项目基于Spring、Netty开发的分布式RPC框架。

- 支持ETCD、ZooKeeper、Redis等多个注册中心
- 服务端异步多线程处理RPC请求
- 客户端使用TCP长连接（多次调用共享连接）
- TCP心跳连接检测
- 支持自定义负载均衡策略
- 支持多种序列化/反序列化策略

### provider
```java
@MiniRpcService(DemoService.class)
public class DemoServiceImpl implements DemoService {
    @Override
    public Integer add(int a, int b) {
        return a + b;
    }
}
```
### consumer
```java
public class TestClient {
    @MiniRpcAutowired
    private DemoService demoService;

    public void test() {
        System.out.println(demoService.add(1, 2));
    }
}
```