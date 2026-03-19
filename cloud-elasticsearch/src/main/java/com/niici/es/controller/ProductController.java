package com.niici.es.controller;

import com.niici.bean.common.Result;
import com.niici.es.Index.Product;
import com.niici.es.dto.ProductSearchRequest;
import com.niici.es.service.ProductSearchService;
import com.niici.es.vo.PageResult;
import com.niici.es.vo.ProductSearchVO;
import jakarta.annotation.Resource;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ProductSearchService productSearchService;

    // ==================== 写入接口 ====================

    @PostMapping("/save")
    public Result save(@RequestBody Product product) {
        return Result.success(productSearchService.save(product));
    }

    @PostMapping("/batch-save")
    public Result batchSave(@RequestBody List<Product> products) {
        productSearchService.batchSave(products);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable String id) {
        productSearchService.delete(id);
        return Result.success();
    }

    // ==================== 查询接口 ====================

    /**
     * 简单查询接口（关键词 + 价格区间 + 分页）
     * GET /product/search?keyword=华为&minPrice=3000&maxPrice=8000&page=0&size=10
     */
    @GetMapping("/search")
    public Result search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        SearchHits<Product> hits = productSearchService.search(keyword, minPrice, maxPrice, page, size);

        List<ProductSearchVO> voList = hits.getSearchHits().stream()
                .map(ProductController::toVO)
                .collect(Collectors.toList());

        return Result.success(PageResult.of(hits.getTotalHits(), voList));
    }

    /**
     * 企业级复合查询（全文检索 + 价格/标签过滤 + 高亮 + 分页排序）
     * POST /product/search/advanced
     */
    @PostMapping("/search/advanced")
    public Result searchAdvanced(@RequestBody ProductSearchRequest request) {
        SearchHits<Product> hits = productSearchService.searchWithRequest(request);

        List<ProductSearchVO> voList = hits.getSearchHits().stream()
                .map(ProductController::toVO)
                .collect(Collectors.toList());

        return Result.success(PageResult.of(hits.getTotalHits(), voList));
    }

    /**
     * 按标签聚合统计
     * GET /product/aggregate/tag
     */
    @GetMapping("/aggregate/tag")
    public Result aggregateByTag() {
        SearchHits<Product> hits = productSearchService.aggregateByTag();
        // 聚合结果从 aggregations 中读取，文档数量为 0（withMaxResults(0)）
        return Result.success(hits.getAggregations());
    }

    // ==================== 私有工具方法 ====================

    /**
     * SearchHit → ProductSearchVO
     * 优先取高亮字段，无高亮则降级为原始值
     */
    private static ProductSearchVO toVO(SearchHit<Product> hit) {
        Product product = hit.getContent();
        ProductSearchVO vo = new ProductSearchVO();

        vo.setId(product.getId());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setTags(product.getTags());
        vo.setCreateTime(product.getCreateTime());
        vo.setScore(hit.getScore());

        // name 高亮：命中时取第一段高亮片段，否则取原值
        List<String> nameHighlight = hit.getHighlightFields().get("name");
        vo.setName((nameHighlight != null && !nameHighlight.isEmpty())
                ? nameHighlight.get(0)
                : product.getName());

        // description 高亮
        List<String> descHighlight = hit.getHighlightFields().get("description");
        vo.setDescription((descHighlight != null && !descHighlight.isEmpty())
                ? descHighlight.get(0)
                : product.getDescription());

        return vo;
    }
}
