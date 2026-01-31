package com.niici.order.service;

import com.niici.bean.order.Order;

public interface OrderService {
    Order createOrder(Long productId, Long userId);

    void getNacosConfig();
}
