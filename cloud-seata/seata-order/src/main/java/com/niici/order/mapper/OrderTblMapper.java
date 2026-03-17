package com.niici.order.mapper;

import com.niici.order.bean.OrderTbl;
import org.apache.ibatis.annotations.Param;

/**
* @author lfy
* @description 针对表【order_tbl】的数据库操作Mapper
* @createDate 2025-01-08 18:34:18
* @Entity com.atguigu.order.bean.OrderTbl
*/
public interface OrderTblMapper {

    int deleteByPrimaryKey(Long id);

    int insert(OrderTbl record);

    int insertSelective(OrderTbl record);

    OrderTbl selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(OrderTbl record);

    int updateByPrimaryKey(OrderTbl record);

    /**
     * TCC Confirm阶段：更新订单状态为已确认
     */
    void confirmOrder(@Param("id") Integer id);

    /**
     * TCC Cancel阶段：更新订单状态为已取消
     */
    void cancelOrder(@Param("id") Integer id);

}
