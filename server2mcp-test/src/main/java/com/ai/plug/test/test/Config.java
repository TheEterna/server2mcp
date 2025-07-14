package com.ai.plug.test.test;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 韩
 * time: 2025/4/23 4:17
 */

@Configuration
public class Config implements WebMvcConfigurer {
    @Autowired
    private Interceptor interceptor;



    @Bean
    public McpSchema.Root getRoot() {
        return new McpSchema.Root("file://C:/Users/han/Desktop/Test", "文件系统");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(interceptor);

    }
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 设置你要允许的网站域名
        config.addAllowedOriginPattern("*");
        //允许跨域发送cookie
        config.setAllowCredentials(true);
        //放行全部原始头信息
        config.addAllowedHeader("*");
        //允许所有请求方法跨域调用
        config.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
