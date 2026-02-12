package com.niici.order.config;

import feign.Logger;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * openFeign 日志配置类
 */
@Configuration
public class FeignClientConfiguration {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    Retryer feignRetryer() {
        // 自定义feignClient重试策略, 参数一次为初始间隔时间、最大间隔时间、最大重试次数
        return new Retryer.Default(200, TimeUnit.SECONDS.toMillis(1), 3);
    }
}
