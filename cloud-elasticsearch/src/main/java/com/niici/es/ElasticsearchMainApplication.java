package com.niici.es;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient // 开启服务注册/发现
@SpringBootApplication
public class ElasticsearchMainApplication {
    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchMainApplication.class, args);
    }
}
