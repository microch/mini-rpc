package io.mini.rpc.server;

import io.mini.rpc.annotation.MiniRpcService;
import io.mini.rpc.server.core.NettyServer;
import io.mini.rpc.registry.ServiceRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class RpcServer extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean {

    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        super(serverAddress, serviceRegistry);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> beans = ctx.getBeansWithAnnotation(MiniRpcService.class);
        if (!CollectionUtils.isEmpty(beans)) {
            for (Object serviceBean : beans.values()) {
                MiniRpcService rpcService = serviceBean.getClass().getAnnotation(MiniRpcService.class);
                String interfaceName = rpcService.value().getName();
                String version = rpcService.version();
                addService(interfaceName, version, serviceBean);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    @Override
    public void destroy() throws Exception {
        super.stop();
    }
}
