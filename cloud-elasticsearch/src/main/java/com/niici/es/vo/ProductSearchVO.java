package com.niici.es.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品搜索结果 VO（含高亮字段）
 */
@Data
public class ProductSearchVO {

    private String id;

    /** 高亮后的商品名称（命中词用 <em> 包裹），无命中则为原值 */
    private String name;

    /** 高亮后的商品描述，无命中则为原值 */
    private String description;

    private BigDecimal price;

    private Integer stock;

    private List<String> tags;

    private LocalDateTime createTime;

    /** ES 相关性评分，分值越高代表与搜索词越匹配 */
    private Float score;
}
