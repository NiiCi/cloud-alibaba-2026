package com.niici.order.exception;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.niici.bean.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

/**
 * order 自定义限流异常处理 - 可参考DefaultBlockExceptionHandler
 */
@Component // 注入到容器中
public class OrderBlockExceptionHandler implements BlockExceptionHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        response.setContentType("application/json; charset=utf-8");

        String message = "访问频繁，请稍后再试";
        Integer code = 500;
        if (e instanceof FlowException) {
            message = "接口限流";
        } else if (e instanceof DegradeException) {
            message = "服务降级";
        } else if (e instanceof ParamFlowException) {
            message = "热点参数限流";
        } else if (e instanceof SystemBlockException) {
            message = "系统规则（负载/CPU/内存）不满足";
        } else if (e instanceof AuthorityException) {
            message = "授权规则不通过";
            code = 403;
        }

        Result error = Result.error(code, message);

        response.getWriter().write(objectMapper.writeValueAsString(error));
        response.getWriter().flush();
        response.getWriter().close();

    }
}
