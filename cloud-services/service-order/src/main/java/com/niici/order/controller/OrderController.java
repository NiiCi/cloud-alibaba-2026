package com.niici.order.controller;

import com.niici.bean.common.Result;
import com.niici.bean.order.Order;
import com.niici.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping("/create")
    public Order createOrder(@RequestParam("userId") Long userId,
                             @RequestParam("productId") Long productId) {
        return orderService.createOrder(productId, userId);
    }

    @GetMapping("/secKill")
    public Order secKill(@RequestParam("userId") Long userId,
                             @RequestParam("productId") Long productId) {
        return orderService.createOrder(productId, userId);
    }

    @GetMapping("/getNacosConfig")
    public void getNacosConfig() {
        orderService.getNacosConfig();
    }

    @GetMapping("/writeDb")
    public Result writeDb() {
        return Result.success("writeDb success...");
    }

    @GetMapping("/readDb")
    public Result readDb() {
        return Result.success("readDb success...");
    }
}
