package com.niici.storage.bean;

import java.io.Serializable;
import lombok.Data;

/**
 * @TableName storage_tbl
 */
@Data
public class StorageTbl implements Serializable {
    private Integer id;

    private String commodityCode;

    private Integer count;

    /** TCC Try阶段冻结的库存数量 */
    private Integer frozenCount;

    private static final long serialVersionUID = 1L;
}