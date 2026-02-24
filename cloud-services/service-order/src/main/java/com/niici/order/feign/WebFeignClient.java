package com.niici.order.feign;

import com.niici.bean.product.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "web-client", url="www.baidu.com") // feign客户端
public interface WebFeignClient {

    // 与三方api的接口定义一致
    @GetMapping("/xxx")
    Product getProduct(@PathVariable("id") Long id);
}
