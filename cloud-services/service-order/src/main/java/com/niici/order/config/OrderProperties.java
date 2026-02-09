package com.niici.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "order") // 配置属性批量绑定在nacos下, 可以无需@RefreshScope就能实现自动刷新
//@RefreshScope // 动态刷新nacos配置
@Data
public class OrderProperties {
    private Long timeOut;

    private Boolean autoConfirm;
}
