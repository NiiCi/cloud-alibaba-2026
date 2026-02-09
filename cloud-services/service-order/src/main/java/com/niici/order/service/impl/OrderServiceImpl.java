package com.niici.order.service.impl;

import com.niici.bean.order.Order;
import com.niici.order.config.OrderProperties;
import com.niici.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RefreshScope // 动态刷新nacos配置
public class OrderServiceImpl implements OrderService {

    @Value("${order.timeout}")
    private Long timeOut;

    @Value("${order.auto-confirm}")
    private Boolean autoConfirm;

    @Resource
    private OrderProperties orderProperties;



    @Override
    public Order createOrder(Long productId, Long userId) {
        return Order.builder()
                .id(1L)
                .totalAmount(new BigDecimal("100"))
                .userId(userId)
                .userName("niici")
                .productList(null)
                .build();
    }


    @Override
    public void getNacosConfig() {
        log.info("refreshScope 动态刷新 timeOut: {}, autoConfirm: {}", timeOut, autoConfirm);
        log.info("configuration properties 动态刷新 timeOut: {}, autoConfirm: {}", orderProperties.getTimeOut(), orderProperties.getAutoConfirm());
    }


    public static void main(String[] args) {
        String aaa =
                """
                {
                    "name": "niici"
                }
                """;
        System.out.println(aaa);

        String mode = "单机模式"    ;
        String result = switch (mode) {
            case "单机模式" -> "standAlone";
            default -> {
               yield "unknown";
            }
        };
    }

    /*private Product getProductRemote(Long productId) {

    }*/


    /*CSR证书签名请求文件生成命令：openssl req -new -newkey rsa:2048 -nodes -keyout server.key -out server.csr

    # 国家代码 - CN
    Country Name (2 letter code) [AU]:CN
    # 省份 - Zhejiang
    State or Province Name (full name) [Some-State]: Zhejiang
    # 城市名称（地级市 / 直辖市）- Hangzhou
    Locality Name (eg, city) []: Hangzhou
    # 组织 / 公司名称（企业 / 机构全称）
    Organization Name (eg, company) [Internet Widgits Pty Ltd]: 稠银金租
    # 部门名称（公司内部的部门，如技术部、财务部）- 技术部：IT Department、财务部：Finance Department
    Organizational Unit Name (eg, section) []: IT Department
    # 申请 HTTPS 证书：填域名（FQDN）、申请代码签名证书：填开发者 / 公司名称；
    Common Name (e.g. server FQDN or YOUR name) []: www.example.com
    # 联系邮箱, CA 机构会通过此邮箱发送证书、通知审核结果
    Email Address []: yourname@example.com

    Please enter the following 'extra' attributes
    to be sent with your certificate request
    # CSR 的挑战密码，用于后续撤销证书、重新申请时的身份验证
    A challenge password []:123456
    # 可选的公司名称, 是对前面Organization Name的补充，几乎所有 CA 都忽略此字段
    An optional company name []:123456*/
}
