package com.ai.plug.core.annotation;

import com.ai.plug.core.register.complete.McpCompleteScanRegistrar;
import com.ai.plug.core.register.prompt.McpPromptScanRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author han
 * @time 2025/6/16 17:04
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(McpCompleteScans.class)
@Import(McpCompleteScanRegistrar.class)
public @interface McpCompleteScan {

    @AliasFor("basePackages")
    String[] value() default {};

    @AliasFor("value")
    String[] basePackages() default {};

    McpCompleteScan.Filter[] includeFilters() default {};

    McpCompleteScan.Filter[] excludeFilters() default {};


    @interface Filter {

        McpCompleteScan.FilterType type() default McpCompleteScan.FilterType.CLASS;

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
