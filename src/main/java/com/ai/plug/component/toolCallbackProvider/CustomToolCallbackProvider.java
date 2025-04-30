package com.ai.plug.component.toolCallbackProvider;

import com.ai.plug.common.annotation.ToolScan;
import com.ai.plug.common.utils.AIUtils;
import com.ai.plug.component.ToolContext;
import com.ai.plug.component.parser.AbstractParser;
import com.ai.plug.component.parser.des.AbstractDesParser;
import com.ai.plug.component.parser.starter.Starter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.util.ToolUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author: han
 * time: 2025/04/2025/4/1 11:59
 * des:
 */

@Slf4j
public class CustomToolCallbackProvider implements ToolCallbackProvider {
    private final Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions;

    @Autowired
    private List<AbstractDesParser> desParserList;

    @Autowired
    private Starter starter;


    private CustomToolCallbackProvider(Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions) {
        Assert.notNull(toolAndDefinitions, "toolAndDefinitions cannot be null");

        this.toolAndDefinitions = toolAndDefinitions;
    }


    private boolean isFunctionalType(Method toolMethod) {
        boolean isFunction = ClassUtils.isAssignable(toolMethod.getReturnType(), Function.class) || ClassUtils.isAssignable(toolMethod.getReturnType(), Supplier.class) || ClassUtils.isAssignable(toolMethod.getReturnType(), Consumer.class);
        if (isFunction) {
            log.warn("Method {} is annotated with @Tool but returns a functional type. This is not supported and the method will be ignored.", toolMethod.getName());
        }

        return isFunction;
    }

    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        if (!duplicateToolNames.isEmpty()) {
            throw new IllegalStateException("Multiple tools with the same name (%s) found in sources: %s".formatted(String.join(", ", duplicateToolNames), this.toolAndDefinitions.keySet().stream().map((o) -> {
                return o.getClass().getName();
            }).collect(Collectors.joining(", "))));
        }
    }

    @Override
    public ToolCallback[] getToolCallbacks() {


        ToolCallback[] toolCallbacks = this.toolAndDefinitions.entrySet().stream().map((toolAndDefinition) -> {

            Object toolBean = toolAndDefinition.getKey();
            ToolContext.ToolRegisterDefinition toolDefinition = toolAndDefinition.getValue();

            // 把所有方法获取出来
            return Stream.of(ReflectionUtils.getDeclaredMethods(AopUtils.isAopProxy(toolBean) ? AopUtils.getTargetClass(toolBean) : toolBean.getClass())).filter((toolMethod) -> {

                // 看一下配置
                ToolScan.ToolFilter[] excludeToolFilters = toolDefinition.getExcludeFilters();
                ToolScan.ToolFilter[] includeToolFilters = toolDefinition.getIncludeFilters();


                if (!CollectionUtils.isEmpty(List.of(excludeToolFilters))) {
                    // 如果不为空 就开始遍历
                    for (ToolScan.ToolFilter excludeToolFilter : excludeToolFilters) {
                        boolean isFilter = doFilter(toolMethod, excludeToolFilter);
                        if (isFilter) {
                            // 拦截到了
                            return false;
                        }
                    }
                }

                if (!CollectionUtils.isEmpty(List.of(includeToolFilters))) {
                    // 如果不为空 就开始遍历
                    for (ToolScan.ToolFilter includeToolFilter : includeToolFilters) {
                        boolean isFilter = doFilter(toolMethod, includeToolFilter);
                        if (isFilter) {
                            // 拦截到了
                            return true;
                        }
                    }

                }
                else {
                    // 如果没有拦截到 也没有includeFilter的 就全部放行
                    return true;
                }

                return false;
            }).filter((toolMethod) -> {
                // 过滤函数式方法
                return !this.isFunctionalType(toolMethod);
            }).map((toolMethod) -> {

//                return DefaultToolDefinition.builder().name(ToolUtils.getToolName(method)).description(ToolUtils.getToolDescription(method)).inputSchema(JsonSchemaGenerator.generateForMethodInput(method, new JsonSchemaGenerator.SchemaOption[0]));

                return MethodToolCallback.builder()
                        // 这个ToolDefinition需要重写
                        .toolDefinition(AIUtils.buildToolDefinition(toolMethod, desParserList, starter))
                        .toolMetadata(ToolMetadata.from(toolMethod))
                        .toolMethod(toolMethod)
                        .toolObject(toolBean)
                        .toolCallResultConverter(ToolUtils.getToolCallResultConverter(toolMethod)).build();

            }).toArray((item) -> {
                return new ToolCallback[item];
            });
        }).flatMap(Stream::of).toArray((item) -> {
            return new ToolCallback[item];
        });
        this.validateToolCallbacks(toolCallbacks);
        return toolCallbacks;
    }

    private boolean doFilter(Method toolMethod, ToolScan.ToolFilter includeToolFilter) {
        Class<?>[] includeClasses = includeToolFilter.value();
        Annotation annotation = null;

        switch (includeToolFilter.type()) {
            case ANNOTATION -> {
                for (Class<?> includeClass : includeClasses) {

                    if (!Annotation.class.isAssignableFrom(includeClass)) {
                        // 不是注解抛异常
                        throw new IllegalArgumentException("The passed class: " + includeClass.getName() + " is not an annotation type");
                    }
                    annotation = AnnotationUtils.getAnnotation(toolMethod, (Class<? extends Annotation>) includeClass);
                }
            }
            case META_ANNOTATION -> {
                for (Class<?> includeClass : includeClasses) {

                    if (!Annotation.class.isAssignableFrom(includeClass)) {
                        // 不是注解抛异常
                        throw new IllegalArgumentException("The passed class: " + includeClass.getName() + " is not an annotation type");
                    }
                    annotation = AnnotationUtils.findAnnotation(toolMethod, (Class<? extends Annotation>) includeClass);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + includeToolFilter.type());
        }
        return annotation != null;
    }


    public static CustomToolCallbackProvider.Builder builder() {
        return new CustomToolCallbackProvider.Builder();
    }

    public static class Builder {

        private Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions;

        private Builder() {
        }



        public CustomToolCallbackProvider.Builder toolObjects(Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions) {

            if (toolAndDefinitions == null) {
                log.debug("No information on toolAndDefinitions");
            }
            this.toolAndDefinitions = toolAndDefinitions;
            return this;
        }

        public CustomToolCallbackProvider build() {
            return new CustomToolCallbackProvider(this.toolAndDefinitions);
        }
    }



}
