package com.niici.product.service.impl;

import com.niici.bean.product.Product;
import com.niici.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Override
    public Product getById(Long id) {
        log.info("test load balance");
        // test feign client 默认请求超时60s
        try {
            Thread.sleep(61000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Product.builder()
                .id(1L)
                .price(new BigDecimal("100"))
                .productName("奶嘴")
                .number(20).build();
    }
}
