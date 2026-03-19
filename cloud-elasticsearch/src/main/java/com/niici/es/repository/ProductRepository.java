package com.niici.es.repository;

import com.niici.es.Index.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, String> {

    // 名称全文匹配（分页）
    Page<Product> findByNameContaining(String name, Pageable pageable);

    // 价格区间
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // 标签精确匹配
    List<Product> findByTags(String tag);

    // 库存大于指定値且包含指定标签
    List<Product> findByStockGreaterThanAndTags(Integer stock, String tag);
}
