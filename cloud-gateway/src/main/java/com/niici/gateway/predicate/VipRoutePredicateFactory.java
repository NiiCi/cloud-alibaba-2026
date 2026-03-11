package com.niici.gateway.predicate;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;


@Component
public class VipRoutePredicateFactory extends AbstractRoutePredicateFactory<VipRoutePredicateFactory.Config> {

    private final static String PARAM_KEY = "param";

    private final static String VALUE_KEY = "value";

    /**
     * 无参构造(必须)
     */
    public VipRoutePredicateFactory() {
        super(Config.class);
    }

    /**
     * 用于定义短写法下, 参数的顺序
     * @return
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(PARAM_KEY, VALUE_KEY);
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return new GatewayPredicate() {
            // test用于判断是否匹配 param对应的key是否匹配指定的value或者规则
            @Override
            public boolean test(ServerWebExchange exchange) {
                if (!StringUtils.hasText(config.value)) {
                    // check existence of header
                    return exchange.getRequest().getQueryParams().containsKey(config.param);
                }

                List<String> values = exchange.getRequest().getQueryParams().get(config.param);
                if (values == null) {
                    return false;
                }
                for (String value : values) {
                    if (value != null && value.matches(config.value)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Object getConfig() {
                return config;
            }

            @Override
            public String toString() {
                return String.format("Vip: param=%s value=%s", config.getParam(), config.getValue());
            }
        };
    }

    /**
     * 可配置的参数, param为key, value为value
     */
    @Validated
    public static class Config {

        @NotEmpty
        private String param;

        @NotEmpty
        private String value;

        public String getParam() {
            return param;
        }

        public VipRoutePredicateFactory.Config setParam(String param) {
            this.param = param;
            return this;
        }

        public String getValue() {
            return value;
        }

        public VipRoutePredicateFactory.Config setValue(String value) {
            this.value = value;
            return this;
        }

    }
}
