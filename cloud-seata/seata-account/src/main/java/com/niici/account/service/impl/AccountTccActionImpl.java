package com.niici.account.service.impl;

import com.niici.account.mapper.AccountTblMapper;
import com.niici.account.service.AccountTccAction;
import io.seata.rm.tcc.api.BusinessActionContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账户 TCC Action 实现
 * useTCCFence=true 已在框架层面保证幂等性、空回滚、悬挂防护，业务代码只需关注正常逻辑
 */
@Slf4j
@Service
public class AccountTccActionImpl implements AccountTccAction {

    @Resource
    AccountTblMapper accountTblMapper;

    /**
     * Try 阶段：预扣减余额（可用余额 -money，冻结余额 +money）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryDebit(BusinessActionContext actionContext, String userId, int money) {
        log.info("AccountTcc Try阶段 - 冻结余额: userId={}, money={}, xid={}",
                userId, money, actionContext.getXid());
        accountTblMapper.tryDebit(userId, money);
        // Seata 2.x 对 primitive int 序列化存在问题，显式存储数值参数确保 confirm/cancel 可正确读取
        actionContext.addActionContext("money", String.valueOf(money));
        return true;
    }

    /**
     * Confirm 阶段：清除冻结余额（冻结余额 -money），正式提交
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(BusinessActionContext actionContext) {
        String userId = (String) actionContext.getActionContext("userId");
        Object moneyObj = actionContext.getActionContext("money");
        int money = moneyObj != null ? Integer.parseInt(moneyObj.toString()) : 0;
        log.info("AccountTcc Confirm阶段 - 确认扣减冻结余额: userId={}, money={}, xid={}",
                userId, money, actionContext.getXid());
        accountTblMapper.confirmDebit(userId, money);
        return true;
    }

    /**
     * Cancel 阶段：释放冻结余额（可用余额 +money，冻结余额 -money），回滚预扣减
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(BusinessActionContext actionContext) {
        String userId = (String) actionContext.getActionContext("userId");
        Object moneyObj = actionContext.getActionContext("money");
        int money = moneyObj != null ? Integer.parseInt(moneyObj.toString()) : 0;
        log.info("AccountTcc Cancel阶段 - 释放冻结余额: userId={}, money={}, xid={}",
                userId, money, actionContext.getXid());
        accountTblMapper.cancelDebit(userId, money);
        return true;
    }
}
