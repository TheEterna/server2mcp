package com.ai.plug.core.provider;

import com.ai.plug.core.annotation.McpTool;
import com.ai.plug.core.annotation.ToolScan;
import com.ai.plug.core.builder.ToolDefinitionBuilder;
import com.ai.plug.core.context.root.IRootContext;
import com.ai.plug.core.context.tool.ToolContext;
import com.ai.plug.common.utils.JsonParser;
import com.ai.plug.core.spec.callback.tool.AsyncMcpToolMethodCallback;
import com.ai.plug.core.spec.callback.tool.DefaultMcpCallToolResultConverter;
import com.ai.plug.core.spec.callback.tool.McpCallToolResultConverter;
import com.ai.plug.core.spec.callback.tool.SyncMcpToolMethodCallback;
import com.ai.plug.core.tenant.TenantContext;
import com.ai.plug.core.tenant.TenantPolicy;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.*;
import io.modelcontextprotocol.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.*;
import tools.jackson.core.type.TypeReference;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;
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

    private IRootContext rootContext;

    public McpToolProvider(Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions, ToolDefinitionBuilder toolDefinitionBuilder, IRootContext rootContext) {
        Assert.notNull(toolAndDefinitions, "toolAndDefinitions cannot be null");
        this.toolAndDefinitions = toolAndDefinitions;
        this.toolDefinitionBuilder = toolDefinitionBuilder;
        this.rootContext = rootContext;
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
                            // 多租户隔离：denyAll=true 工具不出现在 list
                            .filter((toolMethod) -> TenantPolicy.isVisible(
                                toolMethod.getAnnotation(McpTool.class), TenantContext.get()))
                            // 过滤函数式方法
                            .filter(toolMethod -> !isFunctionalType(toolMethod, log))
                            .map(mcpToolMethod -> {
                                McpTool toolAnnotation = mcpToolMethod.getAnnotation(McpTool.class);

                                Map<String, Object> outputSchema = this.toolDefinitionBuilder.buildToolOutputSchema(mcpToolMethod);

                                ToolDefinition toolInfo = this.toolDefinitionBuilder.buildToolDefinition(mcpToolMethod);
                                // MCP SDK 2.0：Tool.inputSchema 由 McpSchema.JsonSchema 统一为 Map<String, Object>
                                Map<String, Object> inputSchema = this.toolDefinitionBuilder.buildToolInputSchema(toolInfo.inputSchema());

                                // 机理: default title use value of name

                                String title;
                                if (toolAnnotation != null) {
                                    title = StringUtils.hasText(toolAnnotation.title()) ? toolAnnotation.title() : toolInfo.name();
                                } else {
                                    title = toolInfo.name();
                                }

//                                McpSchema.Tool mcpTool = new McpSchema.Tool(toolInfo.name(), title, toolInfo.description(),
//                                        inputSchema, outputSchema, getToolAnnotations(toolAnnotation), null);

                                McpSchema.Tool mcpTool = McpSchema.Tool.builder()
                                        .name(toolInfo.name())
                                        .title(title)
                                        .description(toolInfo.description())
                                        .inputSchema(inputSchema)
                                        .outputSchema(outputSchema)
                                        .annotations(getToolAnnotations(toolAnnotation))
                                        .icons(buildIcons(toolAnnotation))
                                        .meta(buildMeta(toolAnnotation))
                                        .build();


                                AsyncMcpToolMethodCallback methodCallback = AsyncMcpToolMethodCallback.builder()
                                        .method(mcpToolMethod)
                                        .bean(toolObject)
                                        .converter(getConverter(toolAnnotation))
                                        .toolAnnotation(toolAnnotation)
                                        .rootContext(this.rootContext)
                                        .build();

                                // MCP SDK 2.0：AsyncToolSpecification 收敛为 (tool, callHandler) 两参数，
                                // 原中间的 legacy call 参数已移除
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
                            // 多租户隔离：denyAll=true 工具不出现在 list
                            .filter((toolMethod) -> TenantPolicy.isVisible(
                                toolMethod.getAnnotation(McpTool.class), TenantContext.get()))
                            // 过滤函数式方法
                            .filter(toolMethod -> !isFunctionalType(toolMethod, log))
                            .map(mcpToolMethod -> {
                                McpTool toolAnnotation = mcpToolMethod.getAnnotation(McpTool.class);

                                Map<String, Object> outputSchema = this.toolDefinitionBuilder.buildToolOutputSchema(mcpToolMethod);

                                ToolDefinition toolInfo = this.toolDefinitionBuilder.buildToolDefinition(mcpToolMethod);
                                // MCP SDK 2.0：Tool.inputSchema 由 McpSchema.JsonSchema 统一为 Map<String, Object>
                                Map<String, Object> inputSchema = this.toolDefinitionBuilder.buildToolInputSchema(toolInfo.inputSchema());


                                // 机理: default title use value of name
                                String title;
                                if (toolAnnotation != null) {
                                    title = StringUtils.hasText(toolAnnotation.title()) ? toolAnnotation.title() : toolInfo.name();
                                } else {
                                    title = toolInfo.name();
                                }

//                                McpSchema.Tool mcpTool = new McpSchema.Tool(toolInfo.name(), title, toolInfo.description(),
//                                        inputSchema, outputSchema, getToolAnnotations(toolAnnotation), null);

                                McpSchema.Tool mcpTool = McpSchema.Tool.builder()
                                        .name(toolInfo.name())
                                        .title(title)
                                        .description(toolInfo.description())
                                        .inputSchema(inputSchema)
                                        .outputSchema(outputSchema)
                                        .annotations(getToolAnnotations(toolAnnotation))
                                        .icons(buildIcons(toolAnnotation))
                                        .meta(buildMeta(toolAnnotation))
                                        .build();

                                SyncMcpToolMethodCallback methodCallback = SyncMcpToolMethodCallback.builder()
                                        .method(mcpToolMethod)
                                        .bean(toolObject)
                                        .converter(getConverter(toolAnnotation))
                                        .toolAnnotation(toolAnnotation)
                                        .rootContext(this.rootContext)
                                        .build();


                                // MCP SDK 2.0：SyncToolSpecification 收敛为 (tool, callHandler) 两参数
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

        Class<? extends McpCallToolResultConverter> converterClass = (Class<? extends McpCallToolResultConverter>) toolAnnotation.converter();
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

        // the title is confilicated with the toolAnnotation.name(), so i guess the point will be fixed
        String title = toolAnnotation.title();
        boolean readOnlyHint = toolAnnotation.readOnlyHint();
        boolean destructiveHint = toolAnnotation.destructiveHint();
        boolean idempotentHint = toolAnnotation.idempotentHint();
        boolean openWorldHint = toolAnnotation.openWorldHint();
        boolean returnDirect = toolAnnotation.returnDirect();
        // listChanged is now part of McpSchema.ToolAnnotations for sync server
        // — we surface the new @McpTool.listChanged() flag. (SDK 2.0 doesn't
        // include this field on the record; the value flows into customizer
        // and McpToolChangeNotifier for change-notification policy.)

        McpSchema.ToolAnnotations toolAnnotations = new McpSchema.ToolAnnotations(title, readOnlyHint, destructiveHint, idempotentHint, openWorldHint, returnDirect);

        return toolAnnotations;
    }

    /**
     * Whether the tool's tool list is dynamic — extracted from {@link McpTool#listChanged()}.
     * Exposed for {@link com.ai.plug.core.spec.change.McpToolChangeNotifier}
     * to decide whether to fire change notifications for this tool. Default
     * {@code true}.
     */
    protected boolean isListChanged(McpTool toolAnnotation) {
        return toolAnnotation == null || toolAnnotation.listChanged();
    }

    /**
     * 解析 {@link McpTool#icons()} 为 SDK 2.0 {@code Tool.icons} 字段。
     * 数组元素格式：{@code src[|mimeType[|sizes[|theme]]]}（竖线分隔，便于 Jackson/Swagger
     * 不必理解本项目私有约定；空字段省略）。{@code icons()} 为空数组或全空字符串时返回 {@code null}，
     * 以避免向 Tool 写入空 list 触发协议侧「空但存在」的语义歧义。
     */
    protected List<McpSchema.Icon> buildIcons(McpTool toolAnnotation) {
        if (toolAnnotation == null) {
            return null;
        }
        String[] raw = toolAnnotation.icons();
        if (raw.length == 0) {
            return null;
        }
        List<McpSchema.Icon> icons = new ArrayList<>(raw.length);
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split("\\|", -1);
            String src = parts[0].trim();
            if (src.isEmpty()) {
                continue;
            }
            McpSchema.Icon.Builder b = McpSchema.Icon.builder(src);
            if (parts.length > 1 && !parts[1].isBlank()) {
                b.mimeType(parts[1].trim());
            }
            if (parts.length > 2 && !parts[2].isBlank()) {
                b.sizes(List.of(parts[2].trim().split(",")));
            }
            if (parts.length > 3 && !parts[3].isBlank()) {
                b.theme(parts[3].trim());
            }
            icons.add(b.build());
        }
        return icons.isEmpty() ? null : icons;
    }

    /**
     * 解析 {@link McpTool#metaJson()} 为 SDK 2.0 {@code Tool.meta} 字段。
     * 接受任意 JSON 对象字符串，空时返回 {@code null}。解析失败抛 {@link IllegalArgumentException}
     * ——metaJson 是用户显式声明，过宽松会掩盖配置错误。
     */
    protected Map<String, Object> buildMeta(McpTool toolAnnotation) {
        if (toolAnnotation == null) {
            return null;
        }
        String metaJson = toolAnnotation.metaJson();
        if (metaJson == null || metaJson.isBlank()) {
            return null;
        }
        try {
            return JsonParser.fromJson(metaJson, new TypeReference<Map<String, Object>>() {
            });
        }
        catch (Exception ex) {
            throw new IllegalArgumentException("@McpTool.metaJson 解析失败：必须是合法 JSON 对象字符串。原始值：" + metaJson, ex);
        }
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
