package com.niici.order.bean;

import java.io.Serializable;
import lombok.Data;

/**
 * @TableName order_tbl
 */
@Data
public class OrderTbl implements Serializable {
    private Integer id;

    private String userId;

    private String commodityCode;

    private Integer count;

    private Integer money;

    /** 订单状态: 0-TCC Try预创建, 1-Confirm已确认, 2-Cancel已取消 */
    private Integer status;

    private static final long serialVersionUID = 1L;
}