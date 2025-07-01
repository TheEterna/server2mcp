package com.ai.plug.core.provider;

import com.ai.plug.core.annotation.ToolScan;
import com.ai.plug.core.builder.ToolDefinitionBuilder;
import com.ai.plug.core.context.ToolContext;
import com.ai.plug.core.utils.CustomToolUtil;
import com.logaritex.mcp.annotation.McpTool;
import com.logaritex.mcp.method.tool.AsyncMcpToolMethodCallback;
import com.logaritex.mcp.method.tool.DefaultMcpCallToolResultConverter;
import com.logaritex.mcp.method.tool.McpCallToolResultConverter;
import com.logaritex.mcp.method.tool.SyncMcpToolMethodCallback;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.ai.plug.common.utils.AssetUtils.isFunctionalType;

/**
 * @author han
 * @time 2025/6/29 21:02
 */

public class McpToolProvider {

    private static final Logger log = LoggerFactory.getLogger(McpToolProvider.class);

    private final Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions;


    private ToolDefinitionBuilder toolDefinitionBuilder;

    public McpToolProvider(Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions, ToolDefinitionBuilder toolDefinitionBuilder) {
        Assert.notNull(toolAndDefinitions, "toolAndDefinitions cannot be null");
        this.toolAndDefinitions = toolAndDefinitions;
        this.toolDefinitionBuilder = toolDefinitionBuilder;
    }

    /**
     * Returns a list of tool specifications for async tool methods.
     * @return List<McpServerFeatures.AsyncToolSpecification>
     */
    public List<McpServerFeatures.AsyncToolSpecification> getAsyncToolSpecifications() {

        List<McpServerFeatures.AsyncToolSpecification> methodCallbacks = this.toolAndDefinitions.entrySet().stream()
                .map(toolAndDefinition -> {
                    Object toolObject = toolAndDefinition.getKey();
                    ToolContext.ToolRegisterDefinition toolDefinition = toolAndDefinition.getValue();

                    return Stream.of(doGetClassMethods(toolObject))
                            // scan 过滤
                            .filter((toolMethod) -> doToolFilter(toolMethod, toolDefinition))
                            // 过滤函数式方法
                            .filter(toolMethod -> !isFunctionalType(toolMethod, log))
                            .map(mcpToolMethod -> {
                                McpTool toolAnnotation = mcpToolMethod.getAnnotation(McpTool.class);

                                ToolDefinition toolInfo = this.toolDefinitionBuilder.buildToolDefinition(mcpToolMethod);
                                McpSchema.Tool mcpTool = new McpSchema.Tool(toolInfo.name(), toolInfo.description(),
                                        toolInfo.inputSchema(), getToolAnnotations(toolAnnotation));


                                AsyncMcpToolMethodCallback methodCallback = AsyncMcpToolMethodCallback.builder()
                                        .method(mcpToolMethod)
                                        .bean(toolObject)
                                        .toolAnnotation(toolAnnotation)
                                        .build();

                                return new McpServerFeatures.AsyncToolSpecification(mcpTool, methodCallback);
                            })
                            .toList();

                })
                .flatMap(List::stream)
                .toList();

        return methodCallbacks;
    }

    /**
     * Returns a list of tool specifications for sync tool methods.
     * @return List<McpServerFeatures.SyncToolSpecification>
     */
    public List<McpServerFeatures.SyncToolSpecification> getSyncToolSpecifications() {

        List<McpServerFeatures.SyncToolSpecification> methodCallbacks = this.toolAndDefinitions.entrySet().stream()
                .map(toolAndDefinition -> {
                    Object toolObject = toolAndDefinition.getKey();
                    ToolContext.ToolRegisterDefinition toolDefinition = toolAndDefinition.getValue();

                    return Stream.of(doGetClassMethods(toolObject))
                            // scan 过滤
                            .filter((toolMethod) -> doToolFilter(toolMethod, toolDefinition))
                            // 过滤函数式方法
                            .filter(toolMethod -> !isFunctionalType(toolMethod, log))
                            .map(mcpToolMethod -> {
                                McpTool toolAnnotation = mcpToolMethod.getAnnotation(McpTool.class);

                                ToolDefinition toolInfo = this.toolDefinitionBuilder.buildToolDefinition(mcpToolMethod);
                                McpSchema.Tool mcpTool = new McpSchema.Tool(toolInfo.name(), toolInfo.description(),
                                        toolInfo.inputSchema(), getToolAnnotations(toolAnnotation));


                                SyncMcpToolMethodCallback methodCallback = SyncMcpToolMethodCallback.builder()
                                        .method(mcpToolMethod)
                                        .bean(toolObject)
                                        .converter(getConverter(toolAnnotation))
                                        .toolAnnotation(toolAnnotation)
                                        .build();


                                return new McpServerFeatures.SyncToolSpecification(mcpTool, methodCallback);
                            })
                            .toList();

                })
                .flatMap(List::stream)
                .toList();

        return methodCallbacks;
    }

    /**
     * Returns the methods of the given bean class.
     * @param bean the bean instance
     * @return the methods of the bean class
     */
    protected Method[] doGetClassMethods(Object bean) {

        Method[] methods = bean.getClass().getDeclaredMethods();
        Arrays.sort(methods, Comparator
                .comparing(Method::getName)
                .thenComparing(method -> Arrays.toString(method.getParameterTypes())));
        return methods;
    }


    protected McpCallToolResultConverter getConverter(McpTool toolAnnotation) {

        if (toolAnnotation == null) {
            return new DefaultMcpCallToolResultConverter();
        }

        Class<? extends McpCallToolResultConverter> converterClass = toolAnnotation.converter();
        McpCallToolResultConverter converter = null;
        try {
            converter = converterClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate ToolCallResultConverter: " + converterClass, e);
        }
        return converter;
    }
    protected McpSchema.ToolAnnotations getToolAnnotations(McpTool toolAnnotation) {
        if (toolAnnotation == null) {
            return null;
        }
        String title = toolAnnotation.title();
        boolean readOnlyHint = toolAnnotation.readOnlyHint();
        boolean destructiveHint = toolAnnotation.destructiveHint();
        boolean idempotentHint = toolAnnotation.idempotentHint();
        boolean openWorldHint = toolAnnotation.openWorldHint();
        boolean returnDirect = toolAnnotation.returnDirect();

        McpSchema.ToolAnnotations toolAnnotations = new McpSchema.ToolAnnotations(title, readOnlyHint, destructiveHint, idempotentHint, openWorldHint, returnDirect);

        return toolAnnotations;
    }

    private static String getName(Method method, McpTool tool) {
        Assert.notNull(method, "method cannot be null");
        if (tool == null || tool.name() == null || tool.name().isEmpty()) {
            return method.getName();
        }
        return tool.name();
	}

    private boolean doToolFilter(Method toolMethod, ToolContext.ToolRegisterDefinition toolDefinition) {
        // 看一下配置
        ToolScan.ToolFilter[] excludeToolFilters = toolDefinition.getExcludeFilters();
        ToolScan.ToolFilter[] includeToolFilters = toolDefinition.getIncludeFilters();


        if (excludeToolFilters != null && excludeToolFilters.length != 0 && !CollectionUtils.isEmpty(List.of(excludeToolFilters))) {
            // 如果不为空 就开始遍历
            for (ToolScan.ToolFilter excludeToolFilter : excludeToolFilters) {
                boolean isFilter = doFilter(toolMethod, excludeToolFilter);
                if (isFilter) {
                    // 拦截到了
                    return false;
                }
            }
        }

        if (includeToolFilters != null && includeToolFilters.length != 0 &&!CollectionUtils.isEmpty(List.of(includeToolFilters))) {
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
            default -> {
                throw new IllegalStateException("Unexpected value: " + includeToolFilter.type());
            }
        }
        return annotation != null;
    }
}
