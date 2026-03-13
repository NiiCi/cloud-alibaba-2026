package com.niici.order.service.impl;

import com.niici.order.bean.OrderTbl;
import com.niici.order.feign.AccountFeignClient;
import com.niici.order.mapper.OrderTblMapper;
import com.niici.order.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {
    @Resource
    OrderTblMapper orderTblMapper;
    @Resource
    AccountFeignClient accountFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderTbl create(String userId, String commodityCode, int orderCount) {
        // 1、计算订单价格
        int orderMoney = calculate(commodityCode, orderCount);

        // 2.扣减余额
        accountFeignClient.debit(userId, orderMoney);

        //3、保存订单
        OrderTbl orderTbl = new OrderTbl();
        orderTbl.setUserId(userId);
        orderTbl.setCommodityCode(commodityCode);
        orderTbl.setCount(orderCount);
        orderTbl.setMoney(orderMoney);

        orderTblMapper.insert(orderTbl);

        // 模拟异常
        int i = 10/0;

        return orderTbl;
    }

    // 计算价格
    private int calculate(String commodityCode, int orderCount) {
        return 9*orderCount;
    }
}
