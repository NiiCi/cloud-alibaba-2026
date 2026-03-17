package com.niici.business.service.impl;

import com.niici.business.feign.OrderFeignClient;
import com.niici.business.feign.StorageFeignClient;
import com.niici.business.service.BusinessService;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BusinessServiceImpl implements BusinessService {
    @Resource
    private StorageFeignClient storageFeignClient;
    @Resource
    private OrderFeignClient orderFeignClient;

    /**
     * 购买商品 -> 总体链路事务: 扣减库存 -> 创建订单 -> 扣减余额（扣减余额在创建订单的方法中进行调用）
     * @param userId            用户id
     * @param commodityCode     商品编号
     * @param orderCount        购买数量
     */
    @Override
    // seata全局事务, 开启后可控制该方法下所有远程方法的事务
    @GlobalTransactional
    public void purchase(String userId, String commodityCode, int orderCount) {
        // 1. 扣减库存
        storageFeignClient.deduct(commodityCode, orderCount); // debug调试入口
        // 2. 创建订单
        orderFeignClient.create(userId, commodityCode, orderCount);
    }
}
