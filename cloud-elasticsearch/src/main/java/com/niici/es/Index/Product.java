package com.niici.es.Index;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(indexName = "product_index", createIndex = true) // 声明所属的索引, 以及是否需要创建索引, 若索引存在, 则不创建, 否则创建
@Setting(shards = 3, replicas = 1) // 声明索引的分片和副本
public class Product {

    @Id
    private String id;

    /**
     * text：全文检索，写入时用 ik_max_word 细粒度分词，搜索时用 ik_smart 粗粒度分词
     * keyword 子字段：精确匹配 / 排序 / 聚合，通过 name.keyword 访问
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)}
    )
    private String name;

    /**
     * ES 内部将值乘以 scalingFactor 后以 long 存储。
     * scalingFactor = 100：精确到小数点后 2 位（适合金额，如 99.99 存为 9999）
     * scalingFactor = 1000：精确到小数点后 3 位
     */
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime createTime;
}
