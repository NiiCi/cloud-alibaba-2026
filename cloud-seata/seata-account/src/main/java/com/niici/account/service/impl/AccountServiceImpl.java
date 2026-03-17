package com.niici.account.service.impl;

import com.niici.account.mapper.AccountTblMapper;
import com.niici.account.service.AccountService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    AccountTblMapper accountTblMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void debit(String userId, int money) {
        // 扣减账户余额
        accountTblMapper.debit(userId,money);
    }
}
