package com.niici.order.service.impl;

import com.niici.bean.order.Order;
import com.niici.bean.product.Product;
import com.niici.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Value("${order.timeout}")
    private Long timeOut;

    @Value("${order.auto-confirm}")
    private Boolean autoConfirm;



    @Override
    public Order createOrder(Long productId, Long userId) {
        return Order.builder()
                .id(1L)
                .totalAmount(new BigDecimal("100"))
                .userId(userId)
                .userName("niici")
                .productList(null)
                .build();
    }


    @Override
    public void getNacosConfig() {
        log.info("timeOut: {}, autoConfirm: {}", timeOut, autoConfirm);
    }

    /*private Product getProductRemote(Long productId) {

    }*/
}
