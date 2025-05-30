package com.ai.plug.core.annotation;

import com.ai.plug.core.register.ToolScanRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;


/**
 * @author: han
 * time: 2025/04/2025/4/2 22:29
 * des:
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ToolScans.class)
@Import(ToolScanRegistrar.class)
public @interface ToolScan {

    @AliasFor("basePackages")
    String[] value() default {};

    @AliasFor("value")
    String[] basePackages() default {};

    Filter[] includeFilters() default {};

    Filter[] excludeFilters() default {};

    ToolFilter[] excludeToolFilters() default {};
    ToolFilter[] includeToolFilters() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    public @interface Filter {

        FilterType type() default FilterType.CLASS;

        @AliasFor("classes")
        Class<?>[] value() default {};

        @AliasFor("value")
        Class<?>[] classes() default {};

    }
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    public @interface ToolFilter {

        ToolFilterType type() default ToolFilterType.ANNOTATION;

        @AliasFor("classes")
        Class<?>[] value() default {};

        @AliasFor("value")
        Class<?>[] classes() default {};

    }

    enum FilterType {
        CLASS, ANNOTATION;
    }
    enum ToolFilterType {
        ANNOTATION, META_ANNOTATION;
    }


}
