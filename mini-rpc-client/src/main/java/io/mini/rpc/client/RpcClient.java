package io.mini.rpc.client;

import io.mini.rpc.annotation.MiniRpcAutowired;
import io.mini.rpc.client.connect.ConnectionManager2;
import io.mini.rpc.registry.ServiceDiscovery;
import io.mini.rpc.client.proxy.ObjectProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class RpcClient implements ApplicationContextAware, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            16, 16, 600L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000));

    private final ServiceDiscovery serviceDiscovery;

    public RpcClient(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        String[] beanNames = ctx.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = ctx.getBean(beanName);
            Field[] fields = bean.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    MiniRpcAutowired annotation = field.getAnnotation(MiniRpcAutowired.class);
                    if (annotation != null) {
                        String version = annotation.version();
                        field.setAccessible(true);
                        field.set(bean, createService(field.getType(), version));
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error(e.toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Object createService(Class<T> interfaceClass, String version) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new ObjectProxy(version));
    }

    public void stop() {
        ConnectionManager2.getInstance().stop();
    }

    @Override
    public void destroy() throws Exception {
        threadPoolExecutor.shutdown();
        serviceDiscovery.stop();
        stop();
    }

    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }
}
