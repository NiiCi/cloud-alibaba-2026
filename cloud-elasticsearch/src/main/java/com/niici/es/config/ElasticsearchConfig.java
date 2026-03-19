package com.niici.es.config;

import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

import java.time.Duration;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Value("${es.pool.max-conn-total:100}")
    private int maxConnTotal;

    @Value("${es.pool.max-conn-per-route:20}")
    private int maxConnPerRoute;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(uris.split(","))
                .withBasicAuth(username, password)
                // 连接超时，单位：秒
                .withConnectTimeout(Duration.ofSeconds(5))
                // 套接字超时，单位：秒
                .withSocketTimeout(Duration.ofSeconds(60))
                // elasticsearch 连接池配置 - yml无法指定 - 只能通过java配置实现
                .withClientConfigurer((RestClientBuilder restClientBuilder) ->
                        restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                                httpClientBuilder
                                        .setMaxConnTotal(maxConnTotal)
                                        .setMaxConnPerRoute(maxConnPerRoute)
                        )
                )
                .build();
    }
}
