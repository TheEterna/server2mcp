package com.ai.plug.test.test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @ author 韩
 * time: 2025/4/23 4:15
 */
@Component
@Slf4j
public class Interceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 打印请求的基本信息
        log.info("请求基本信息：URL: {}, 请求方法: {}, 客户端 IP: {}", request.getRequestURL(), request.getMethod(), request.getRemoteAddr());
        log.info("请求头信息:");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            String headerValue = request.getHeader(headerName);
            log.info("{}: {}", headerName, headerValue);
        });

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}
