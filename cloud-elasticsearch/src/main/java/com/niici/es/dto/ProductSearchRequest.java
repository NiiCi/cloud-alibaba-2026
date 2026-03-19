package com.niici.es.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索请求 DTO
 */
@Data
public class ProductSearchRequest {

    /** 全文检索关键词（搜索 name / description 字段）*/
    private String keyword;

    /** 最低价格（为 null 时不限制）*/
    private BigDecimal minPrice;

    /** 最高价格（为 null 时不限制）*/
    private BigDecimal maxPrice;

    /** 标签精确过滤（多个标签取 IN 并集）*/
    private List<String> tags;

    /** 页码，从 0 开始 */
    private int page = 0;

    /** 每页条数 */
    private int size = 10;
}
