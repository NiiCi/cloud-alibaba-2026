package com.atguigu.account;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@MapperScan("com.atguigu.account.mapper")
@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement // 必须加！Seata 依赖此注解开启事务代理
public class SeataAccountMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeataAccountMainApplication.class, args);
    }
}
