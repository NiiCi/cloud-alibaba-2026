package com.niici.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OrderRestTemplate {

    @Bean
    @LoadBalanced // 开启nacos 负载均衡
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
