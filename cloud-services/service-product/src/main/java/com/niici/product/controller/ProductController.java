package com.niici.product.controller;

import com.niici.bean.product.Product;
import com.niici.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
public class ProductController {

    @Resource
    private ProductService productService;

    /**
     * 查询商户信息
     * @param id
     */
    @GetMapping("/product/{id}")
    public Product getProduct(@PathVariable("id") Long id) {
        return productService.getById(id);
    }
}
