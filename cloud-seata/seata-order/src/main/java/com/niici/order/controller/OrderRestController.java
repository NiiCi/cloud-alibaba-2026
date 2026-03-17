package com.niici.order.controller;

import com.niici.order.service.OrderTccAction;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderRestController {

    @Autowired
    OrderTccAction orderTccAction;

    /**
     * 创建订单（TCC Try 阶段入口）
     */
    @GetMapping("/create")
    public String create(@RequestParam("userId") String userId,
                         @RequestParam("commodityCode") String commodityCode,
                         @RequestParam("count") int orderCount) {
        orderTccAction.tryCreate(new BusinessActionContext(), userId, commodityCode, orderCount);
        return "order create success";
    }
}
