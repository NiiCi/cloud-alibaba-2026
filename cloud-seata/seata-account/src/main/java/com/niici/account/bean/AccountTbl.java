package com.niici.account.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * 
 * @TableName account_tbl
 */
@Data
public class AccountTbl implements Serializable {

    private Integer id;

    private String userId;

    private Integer money;

    /** TCC Try阶段冻结的余额 */
    private Integer frozenMoney;

}