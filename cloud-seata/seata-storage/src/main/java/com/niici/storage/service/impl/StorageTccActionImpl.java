package com.niici.storage.service.impl;

import com.niici.storage.mapper.StorageTblMapper;
import com.niici.storage.service.StorageTccAction;
import io.seata.rm.tcc.api.BusinessActionContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存 TCC Action 实现
 * useTCCFence=true 已在框架层面保证幂等性、空回滚、悬挂防护，业务代码只需关注正常逻辑
 */
@Slf4j
@Service
public class StorageTccActionImpl implements StorageTccAction {

    @Resource
    StorageTblMapper storageTblMapper;

    /**
     * Try 阶段：预扣减库存（可用库存 -count，冻结库存 +count）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryDeduct(BusinessActionContext actionContext, String commodityCode, int count) {
        log.info("StorageTcc Try阶段 - 冻结库存: commodityCode={}, count={}, xid={}",
                commodityCode, count, actionContext.getXid());
        storageTblMapper.tryDeduct(commodityCode, count);
        return true;
    }

    /**
     * Confirm 阶段：清除冻结库存（冻结库存 -count），正式提交
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(BusinessActionContext actionContext) {
        String commodityCode = (String) actionContext.getActionContext("commodityCode");
        int count = Integer.parseInt(actionContext.getActionContext("count").toString());
        log.info("StorageTcc Confirm阶段 - 确认扣减冻结库存: commodityCode={}, count={}, xid={}",
                commodityCode, count, actionContext.getXid());
        storageTblMapper.confirmDeduct(commodityCode, count);
        return true;
    }

    /**
     * Cancel 阶段：释放冻结库存（可用库存 +count，冻结库存 -count），回滚预扣减
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(BusinessActionContext actionContext) {
        String commodityCode = (String) actionContext.getActionContext("commodityCode");
        int count = Integer.parseInt(actionContext.getActionContext("count").toString());
        log.info("StorageTcc Cancel阶段 - 释放冻结库存: commodityCode={}, count={}, xid={}",
                commodityCode, count, actionContext.getXid());
        storageTblMapper.cancelDeduct(commodityCode, count);
        return true;
    }
}
