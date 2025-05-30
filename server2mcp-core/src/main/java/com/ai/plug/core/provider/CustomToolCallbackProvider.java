package com.ai.plug.core.provider;

import com.ai.plug.core.annotation.ToolScan;
import com.ai.plug.core.ToolContext;
import com.ai.plug.core.builder.ToolDefinitionBuilder;
import com.ai.plug.core.parser.des.AbstractDesParser;
import com.ai.plug.core.parser.param.AbstractParamParser;
import com.ai.plug.core.parser.starter.Starter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolUtils;
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

    private List<AbstractDesParser> desParserList;

    private List<AbstractParamParser> paramParserList;

    private Starter starter;

    @Autowired
    public void setDesParserList(List<AbstractDesParser> desParserList) {
        this.desParserList = desParserList;
        log.info("DesParserList have {} parser", desParserList.size());
    }
    @Autowired
    public void setParamParserList(List<AbstractParamParser> paramParserList) {
        this.paramParserList = paramParserList;
        log.info("ParamParserList have {} parser", paramParserList.size());

    }
    @Autowired
    public void setStarter(Starter starter) {
        this.starter = starter;
    }

    private CustomToolCallbackProvider(Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions) {
        Assert.notNull(toolAndDefinitions, "toolAndDefinitions cannot be null");

        this.toolAndDefinitions = toolAndDefinitions;
    }


    private boolean isFunctionalType(Method toolMethod) {
        boolean isFunction = ClassUtils.isAssignable(Function.class, toolMethod.getReturnType())
                || ClassUtils.isAssignable(Supplier.class, toolMethod.getReturnType())
                || ClassUtils.isAssignable(Consumer.class, toolMethod.getReturnType());

        if (isFunction) {
            log.warn("Method {} is annotated with @Tool but returns a functional type. "
                    + "This is not supported and the method will be ignored.", toolMethod.getName());
        }

        return isFunction;
    }


    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        if (!duplicateToolNames.isEmpty()) {
//            return toolCallbacks;
            /* 这个以前的 !duplicateToolNames.isEmpty() 条件下的
                        throw new IllegalStateException("Multiple tools with the same name (%s) found in sources: %s".formatted(String.join(", ", duplicateToolNames), this.toolAndDefinitions.keySet().stream().map((o) -> {
                            return o.getClass().getName();
                       }).collect(Collectors.joining(", "))));*/

            log.warn("Multiple tools with the same name (%s) found in sources: %s".formatted(String.join(", ", duplicateToolNames), this.toolAndDefinitions.keySet().stream().map((o) -> {
                return o.getClass().getName();
            }).collect(Collectors.joining(", "))));

            // todo 目前是默认操作
            log.warn("Perform default operation: The newly defined tool will overwrite the previous tool");


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
                MethodToolCallback toolCallback = MethodToolCallback.builder()
                        // 这个ToolDefinition需要重写
                        .toolDefinition(ToolDefinitionBuilder.buildToolDefinition(toolMethod, desParserList, paramParserList, starter))
                        .toolMetadata(ToolMetadata.from(toolMethod))
                        .toolMethod(toolMethod)
                        .toolObject(toolBean)
                        .toolCallResultConverter(ToolUtils.getToolCallResultConverter(toolMethod)).build();


                return toolCallback;

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


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions;

        private Builder() {
        }



        public Builder toolObjects(Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions) {

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
