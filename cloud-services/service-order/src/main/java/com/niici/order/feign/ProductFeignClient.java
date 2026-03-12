package com.niici.order.feign;

import com.niici.bean.product.Product;
import com.niici.order.feign.fallback.ProductFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-product", fallback = ProductFeignClientFallback.class) // feign客户端
public interface ProductFeignClient {

    // 与远程服务的接口定义需要一致
    //@GetMapping("/api/product/{id}") // 测试gateway predicate断言用, 在测试gateway filter rewritePath时需注释
    @GetMapping("/{id}") // feignClient 不走gateway的 predicate 和 filters, 所以和controller接口定义保持一致即可;
    Product getProduct(@PathVariable("id") Long id);
}
