package com.niici.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@MapperScan("com.niici.order.mapper")
@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement // 必须加！Seata 依赖此注解开启事务代理
@EnableFeignClients(basePackages = "com.niici.order.feign")
public class SeataOrderMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeataOrderMainApplication.class, args);
    }


}
