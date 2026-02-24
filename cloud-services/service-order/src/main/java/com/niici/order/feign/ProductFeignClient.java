package com.niici.order.feign;

import com.niici.bean.product.Product;
import com.niici.order.feign.fallback.ProductFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-product", fallback = ProductFeignClientFallback.class) // feign客户端
public interface ProductFeignClient {

    // 与远程服务的接口定义需要一致
    @GetMapping("/product/{id}")
    Product getProduct(@PathVariable("id") Long id);
}
