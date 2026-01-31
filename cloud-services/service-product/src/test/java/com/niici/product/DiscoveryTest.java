package com.niici.product;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
public class DiscoveryTest {

    @Resource
    private DiscoveryClient discoveryClient;

    @Test
    public void test () {
        System.out.println(discoveryClient.getServices());

        discoveryClient.getServices().stream().forEach(dd -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(dd);
            instances.stream().forEach(item -> {
                System.out.println(item.getHost() + ":" + item.getPort());
            });
        });
    }
}
