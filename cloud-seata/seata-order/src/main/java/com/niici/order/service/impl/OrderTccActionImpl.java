package com.niici.order.service.impl;

import com.niici.order.bean.OrderTbl;
import com.niici.order.feign.AccountFeignClient;
import com.niici.order.mapper.OrderTblMapper;
import com.niici.order.service.OrderTccAction;
import io.seata.rm.tcc.api.BusinessActionContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单 TCC Action 实现
 * useTCCFence=true 已在框架层面保证幂等性、空回滚、悬挂防护，业务代码只需关注正常逻辑
 */
@Slf4j
@Service
public class OrderTccActionImpl implements OrderTccAction {

    @Resource
    OrderTblMapper orderTblMapper;

    @Resource
    AccountFeignClient accountFeignClient;

    /**
     * Try 阶段：调用账户服务冻结余额，预创建订单（status=0）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryCreate(BusinessActionContext actionContext, String userId, String commodityCode, int orderCount) {
        log.info("OrderTcc Try阶段 - 预创建订单: userId={}, commodityCode={}, orderCount={}, xid={}",
                userId, commodityCode, orderCount, actionContext.getXid());
        // 1. 计算订单金额
        int orderMoney = 9 * orderCount;
        // 2. 调用账户服务 TCC Try 阶段（冻结余额）
        accountFeignClient.debit(userId, orderMoney);
        // 3. 预创建订单（status=0 表示 TCC Try 预创建，尚未确认）
        OrderTbl orderTbl = new OrderTbl();
        orderTbl.setUserId(userId);
        orderTbl.setCommodityCode(commodityCode);
        orderTbl.setCount(orderCount);
        orderTbl.setMoney(orderMoney);
        orderTbl.setStatus(0);
        orderTblMapper.insert(orderTbl);
        // 将订单 id 存入 TCC 上下文，供 Confirm/Cancel 阶段使用
        actionContext.addActionContext("orderId", orderTbl.getId());

        // ===== 模拟异常：触发 TCC 全局回滚 =====
        // 此时 storage Try（库存冻结）和 account Try（余额冻结）均已执行
        // 抛出异常后，Seata TC 将依次回调所有参与方的 cancel 方法，验证数据是否完整还原
        int i = 10 / 0;
        // ===== 模拟异常结束 =====

        return true;
    }

    /**
     * Confirm 阶段：更新订单状态为已确认（status=1）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(BusinessActionContext actionContext) {
        Object orderIdObj = actionContext.getActionContext("orderId");
        Integer orderId = orderIdObj != null ? Integer.parseInt(orderIdObj.toString()) : null;
        log.info("OrderTcc Confirm阶段 - 确认订单: orderId={}, xid={}", orderId, actionContext.getXid());
        if (orderId != null) {
            orderTblMapper.confirmOrder(orderId);
        }
        return true;
    }

    /**
     * Cancel 阶段：更新订单状态为已取消（status=2）
     * useTCCFence 已防护空回滚，orderId==null 的防御判断作为额外保险
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(BusinessActionContext actionContext) {
        Object orderIdObj = actionContext.getActionContext("orderId");
        log.info("OrderTcc Cancel阶段 - 取消订单: orderId={}, xid={}", orderIdObj, actionContext.getXid());
        // useTCCFence 已处理空回滚，此处对 orderId==null 做额外防御（Try 未插入订单时不操作）
        if (orderIdObj != null) {
            Integer orderId = Integer.parseInt(orderIdObj.toString());
            orderTblMapper.cancelOrder(orderId);
        }
        return true;
    }
}
