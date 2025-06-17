package com.ai.plug.core.annotation;

import com.ai.plug.core.register.prompt.McpPromptScanRegistrar;
import com.ai.plug.core.register.resource.McpResourceScanRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author 韩
 * time: 2025/6/13 3:35
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(McpPromptScans.class)
@Import(McpPromptScanRegistrar.class)
public @interface McpPromptScan {


    @AliasFor("basePackages")
    String[] value() default {};

    @AliasFor("value")
    String[] basePackages() default {};

    McpPromptScan.Filter[] includeFilters() default {};

    McpPromptScan.Filter[] excludeFilters() default {};


    @interface Filter {

        McpPromptScan.FilterType type() default McpPromptScan.FilterType.CLASS;

        @AliasFor("classes")
        Class<?>[] value() default {};

        @AliasFor("value")
        Class<?>[] classes() default {};

    }


    /**
     * prompt 扫描的类 的过滤方式目前有
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
