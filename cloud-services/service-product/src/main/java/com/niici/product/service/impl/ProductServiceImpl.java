package com.niici.product.service.impl;

import com.niici.bean.product.Product;
import com.niici.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Override
    public Product getById(Long id) {
        log.info("test load balance");
        // test feign client 默认请求超时60s - 测试feign client超时以及重试
        /*try {
            Thread.sleep(61000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/

        // 测试 sentinel 熔断规则 - 慢调用比例
        /*try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/

        // 测试 sentinal 熔断规则 - 异常比例
        int i = 1 / 0;

        return Product.builder()
                .id(1L)
                .price(new BigDecimal("100"))
                .productName("奶嘴")
                .number(20).build();
    }
}
