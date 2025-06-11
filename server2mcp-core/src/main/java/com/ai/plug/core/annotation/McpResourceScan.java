package com.ai.plug.core.annotation;

import com.ai.plug.core.register.resource.McpResourceScanRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author 韩
 * time: 2025/6/3 3:18
 */


@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(McpResourceScans.class)
@Import(McpResourceScanRegistrar.class)
public @interface McpResourceScan {

    @AliasFor("basePackages")
    String[] value() default {};

    @AliasFor("value")
    String[] basePackages() default {};

    McpResourceScan.Filter[] includeFilters() default {};

    McpResourceScan.Filter[] excludeFilters() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    public @interface Filter {

        McpResourceScan.FilterType type() default McpResourceScan.FilterType.CLASS;

        @AliasFor("classes")
        Class<?>[] value() default {};

        @AliasFor("value")
        Class<?>[] classes() default {};

    }


    /**
     * resource 扫描的类 的过滤方式目前有
     * CLASS 按类扫描
     * ANNOTATION 按注解扫描
     */
    enum FilterType {
        /**
         * CLASS 按类扫描
         */
        CLASS,
        /**
         * ANNOTATION 按注解扫描
         */
        ANNOTATION;
    }





}
