package com.niici.order.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * 订单 TCC Action 接口
 * useTCCFence=true 由 Seata 框架自动处理：幂等性、空回滚、悬挂问题
 *
 * <p>@LocalTCC 的作用：</p>
 * <ul>
 *   <li>标记当前接口为 Seata TCC 资源，使 Seata 在 Spring 容器启动时将其注册到 TC（事务协调器）</li>
 *   <li>与 @TwoPhaseBusinessAction 配合，声明 try/confirm/cancel 三阶段方法的绑定关系</li>
 *   <li>必须标注在接口上（而非实现类），Seata 通过 AOP 代理拦截 try 方法，自动完成分支事务的注册与二阶段回调</li>
 *   <li>区别于远程 TCC（跨服务调用），@LocalTCC 用于本地 Bean 调用场景，confirm/cancel 由 Seata TC 直接在本服务内回调</li>
 * </ul>
 */
@LocalTCC
public interface OrderTccAction {

    /**
     * Try 阶段：预创建订单（status=0），并调用账户服务冻结余额
     */
    @TwoPhaseBusinessAction(
            name = "orderTccAction",
            commitMethod = "confirm",
            rollbackMethod = "cancel",
            useTCCFence = true
    )
    boolean tryCreate(BusinessActionContext actionContext,
                      @BusinessActionContextParameter(paramName = "userId") String userId,
                      @BusinessActionContextParameter(paramName = "commodityCode") String commodityCode,
                      @BusinessActionContextParameter(paramName = "orderCount") int orderCount);

    /**
     * Confirm 阶段：更新订单状态为已确认（status=1）
     */
    boolean confirm(BusinessActionContext actionContext);

    /**
     * Cancel 阶段：更新订单状态为已取消（status=2）
     */
    boolean cancel(BusinessActionContext actionContext);
}
