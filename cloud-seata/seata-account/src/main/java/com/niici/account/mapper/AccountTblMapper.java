package com.niici.account.mapper;

import com.niici.account.bean.AccountTbl;
import org.apache.ibatis.annotations.Param;

/**
* @author lfy
* @description 针对表【account_tbl】的数据库操作Mapper
* @createDate 2025-01-08 18:32:50
* @Entity com.atguigu.account.bean.AccountTbl
*/
public interface AccountTblMapper {

    int deleteByPrimaryKey(Long id);

    int insert(AccountTbl record);

    int insertSelective(AccountTbl record);

    AccountTbl selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(AccountTbl record);

    int updateByPrimaryKey(AccountTbl record);

    void debit(String userId, int money);

    /**
     * TCC Try阶段：扣减可用余额，增加冻结余额
     */
    void tryDebit(@Param("userId") String userId, @Param("money") int money);

    /**
     * TCC Confirm阶段：扣减冻结余额（正式提交）
     */
    void confirmDebit(@Param("userId") String userId, @Param("money") int money);

    /**
     * TCC Cancel阶段：恢复可用余额，释放冻结余额
     */
    void cancelDebit(@Param("userId") String userId, @Param("money") int money);
}
