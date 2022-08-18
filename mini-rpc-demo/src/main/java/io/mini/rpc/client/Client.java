package io.mini.rpc.client;

import io.mini.rpc.client.consumer.TestClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author caohao
 * @date 2022/8/18
 */
public class Client {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ClientConfiguration.class);
        TestClient bean = context.getBean(TestClient.class);
        bean.test();

    }
}
