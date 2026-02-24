package com.niici.order.feign.fallback;

import com.niici.bean.product.Product;
import com.niici.order.feign.ProductFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductFeignClientFallback implements ProductFeignClient {

    @Override
    public Product getProduct(Long id) {
        log.info("product feign client fallback ---- 测试降级");
        Product product = new Product();
        product.setId(id);
        product.setProductName("测试降级");
        return product;
    }
}
