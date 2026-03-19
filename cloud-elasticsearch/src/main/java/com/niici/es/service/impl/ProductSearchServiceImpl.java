package com.niici.es.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import com.niici.es.Index.Product;
import com.niici.es.dto.ProductSearchRequest;
import com.niici.es.repository.ProductRepository;
import com.niici.es.service.ProductSearchService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Resource
    private ProductRepository productRepository;

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    // ==================== 写入操作 ====================

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(UUID.randomUUID().toString());
        }
        if (product.getCreateTime() == null) {
            product.setCreateTime(LocalDateTime.now());
        }
        return productRepository.save(product);
    }

    /**
     * 批量写入（比逐条 save 效率高 5-10 倍）
     */
    @Override
    public void batchSave(List<Product> products) {
        products.forEach(p -> {
            if (p.getId() == null) p.setId(UUID.randomUUID().toString());
            if (p.getCreateTime() == null) p.setCreateTime(LocalDateTime.now());
        });
        productRepository.saveAll(products);
    }

    @Override
    public void delete(String id) {
        productRepository.deleteById(id);
    }

    // ==================== 查询操作 ====================

    /**
     * 简单复合查询（兼容原有接口）
     */
    @Override
    public SearchHits<Product> search(String keyword, Double minPrice, Double maxPrice, int page, int size) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 全文检索
        if (StringUtils.hasText(keyword)) {
            boolQuery.must(m -> m.match(mq -> mq
                    .field("name")
                    .query(keyword)));
        }

        // 价格范围过滤（filter 不影响评分，性能更好）
        if (minPrice != null || maxPrice != null) {
            RangeQuery.Builder rangeQuery = new RangeQuery.Builder().field("price");
            if (minPrice != null) rangeQuery.gte(JsonData.of(minPrice));
            if (maxPrice != null) rangeQuery.lte(JsonData.of(maxPrice));
            boolQuery.filter(f -> f.range(rangeQuery.build()));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withPageable(PageRequest.of(page, size))
                .withSort(Sort.by(Sort.Direction.DESC, "_score"))
                .build();

        return elasticsearchOperations.search(query, Product.class);
    }

    /**
     * 企业级复合查询：全文检索 + 价格过滤 + 标签过滤 + 分页排序 + 关键词高亮
     */
    @Override
    public SearchHits<Product> searchWithRequest(ProductSearchRequest req) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // must：同时搜索 name 和 description，影响相关性评分
        if (StringUtils.hasText(req.getKeyword())) {
            boolQuery.must(m -> m.multiMatch(mq -> mq
                    .fields("name", "description")
                    .query(req.getKeyword())));
        }

        // filter：价格范围（不影响评分，结果会被 ES 缓存，性能更好）
        if (req.getMinPrice() != null || req.getMaxPrice() != null) {
            RangeQuery.Builder range = new RangeQuery.Builder().field("price");
            if (req.getMinPrice() != null) range.gte(JsonData.of(req.getMinPrice()));
            if (req.getMaxPrice() != null) range.lte(JsonData.of(req.getMaxPrice()));
            boolQuery.filter(f -> f.range(range.build()));
        }

        // filter：标签精确匹配（terms = IN 查询）
        if (!CollectionUtils.isEmpty(req.getTags())) {
            List<FieldValue> tagValues = req.getTags().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            boolQuery.filter(f -> f.terms(t -> t
                    .field("tags")
                    .terms(tv -> tv.value(tagValues))));
        }

        // 高亮：命中词用 <em> 标签包裹，前端渲染时可直接展示
        HighlightFieldParameters params = HighlightFieldParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build();
        Highlight highlight = new Highlight(List.of(
                new HighlightField("name", params),
                new HighlightField("description", params)
        ));

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withHighlightQuery(new org.springframework.data.elasticsearch.core.query.HighlightQuery(highlight, Product.class))
                .withPageable(PageRequest.of(req.getPage(), req.getSize()))
                .withSort(Sort.by(Sort.Direction.DESC, "_score"))       // 先按相关度
                .withSort(Sort.by(Sort.Direction.DESC, "createTime"))   // 再按时间
                .build();

        return elasticsearchOperations.search(query, Product.class);
    }

    /**
     * 按标签聚合：统计每组数量及平均价格
     * 注意：withMaxResults(0) 表示只返回聚合结果，不返回文档本身
     */
    @Override
    public SearchHits<Product> aggregateByTag() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .withAggregation("tag_group", Aggregation.of(a -> a
                        .terms(t -> t.field("tags").size(20))
                        .aggregations("avg_price", Aggregation.of(sub -> sub
                                .avg(avg -> avg.field("price"))))))
                .withMaxResults(0)
                .build();

        return elasticsearchOperations.search(query, Product.class);
    }
}
