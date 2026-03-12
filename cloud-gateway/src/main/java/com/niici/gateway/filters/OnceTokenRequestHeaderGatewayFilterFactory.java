package com.niici.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

@Component
public class OnceTokenRequestHeaderGatewayFilterFactory extends AbstractNameValueGatewayFilterFactory {
    @Override
    public GatewayFilter apply(NameValueConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                String onceToken = addHeader(exchange, config);
                ServerHttpRequest request = exchange.getRequest().mutate()
                        .headers(httpHeaders -> httpHeaders.add(config.getName(), onceToken)).build();
                return chain.filter(exchange.mutate().request(request).build());
            }

            @Override
            public String toString() {
                return filterToStringCreator(OnceTokenRequestHeaderGatewayFilterFactory.this)
                        .append(config.getName(), config.getValue()).toString();
            }
        };
    }

    String addHeader(ServerWebExchange exchange, NameValueConfig config) {
        final String value = ServerWebExchangeUtils.expand(exchange, config.getValue());
        return switch (value) {
            case "Jwt" -> "Jwt request header add...";
            case "Uuid" -> "uuid request header add...";
            default -> "";
        };
    }
}
