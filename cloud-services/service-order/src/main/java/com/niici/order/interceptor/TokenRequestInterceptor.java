package com.niici.order.interceptor;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 自定义的feignClient请求拦截器 - 用于给请求头添加token
 */
@Component // 注册拦截器到spring容器中 方式二
@Slf4j
public class TokenRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        log.info("feign client 自定义请求拦截器 ------- 添加token");
        requestTemplate.header("Authorization", "Bearer " + UUID.randomUUID().toString().replaceAll("-", ""));
    }
}
