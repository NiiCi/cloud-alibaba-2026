package com.niici.account.controller;

import com.niici.account.service.AccountTccAction;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountRestController {

    @Autowired
    AccountTccAction accountTccAction;

    /**
     * 扣减账户余额（TCC Try 阶段入口）
     */
    @GetMapping("/debit")
    public String debit(@RequestParam("userId") String userId,
                        @RequestParam("money") int money) {
        accountTccAction.tryDebit(new BusinessActionContext(), userId, money);
        return "account debit success";
    }
}
