package com.niici.es.service;

import com.niici.es.Index.Product;
import com.niici.es.dto.ProductSearchRequest;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

public interface ProductSearchService {

    // ==================== 写入操作 ====================

    /** 单条保存（新增 / 更新）*/
    Product save(Product product);

    /** 批量保存 */
    void batchSave(List<Product> products);

    /** 根据 ID 删除 */
    void delete(String id);

    // ==================== 查询操作 ====================

    /** 简单查询（兼容原有接口）*/
    SearchHits<Product> search(String keyword, Double minPrice, Double maxPrice, int page, int size);

    /** 企业级复合查询：全文检索 + 价格过滤 + 标签过滤 + 分页排序 + 高亮 */
    SearchHits<Product> searchWithRequest(ProductSearchRequest request);

    /** 按标签聚合：统计每组数量及平均价格 */
    SearchHits<Product> aggregateByTag();
}
