package com.ai.plug.core.builder;

import com.ai.plug.common.utils.CommonUtil;
import com.ai.plug.core.annotation.McpTool;
import com.ai.plug.core.parser.tool.des.AbstractDesParser;
import com.ai.plug.core.parser.tool.param.AbstractParamParser;
import com.ai.plug.core.parser.tool.starter.AbstractStarter;
import com.ai.plug.core.spec.utils.elicitation.McpElicitation;
import com.ai.plug.core.spec.utils.elicitation.McpElicitationFactory;
import com.ai.plug.core.spec.utils.logging.McpLogger;
import com.ai.plug.core.spec.utils.logging.McpLoggerFactory;
import com.ai.plug.core.spec.utils.root.McpRoot;
import com.ai.plug.core.spec.utils.root.McpRootFactory;
import com.ai.plug.core.spec.utils.sampling.McpSampling;
import com.ai.plug.core.spec.utils.sampling.McpSamplingFactory;
import com.ai.plug.core.utils.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.modelcontextprotocol.json.*;
import io.modelcontextprotocol.json.jackson3.*;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import com.ai.plug.common.utils.JsonParser;
import tools.jackson.core.type.TypeReference;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.core.ResolvableType;

import static com.ai.plug.common.utils.JsonParser.getObjectMapper;
import static com.ai.plug.core.utils.GenSchemaUtils.MCP_SCHEMA_GENERATOR;
import static com.ai.plug.core.utils.GenSchemaUtils.TOOL_SCHEMA_GENERATOR;


/**
 * @author: han
 * time: 2025/04/2025/4/1 15:40
 * des:
 */

@Slf4j
public class ToolDefinitionBuilder {

    private List<AbstractDesParser> desParserList;
    private List<AbstractParamParser> paramParserList;
    private AbstractStarter starter;

    @Autowired
    public ToolDefinitionBuilder(List<AbstractDesParser> desParserList, List<AbstractParamParser> paramParserList, AbstractStarter starter) {
        this.desParserList = desParserList;
        this.paramParserList = paramParserList;
        this.starter = starter;
    }



    /**
     * 构造Tool定义数据对象
     */
    public ToolDefinition buildToolDefinition(Method toolMethod) {
        Assert.notNull(toolMethod, "方法不能为空");
      

        return DefaultToolDefinition.builder()
                .name(this.getToolName(toolMethod))
                .description(this.getToolDescription(toolMethod, this.desParserList))
                .inputSchema(this.getInputSchema(this.paramParserList, toolMethod)).build();

    }

    public String getToolName(Method method) {

        String className = method.getDeclaringClass().getSimpleName();

        String presentToolName;
        // 把本来的操作作为备选
        Assert.notNull(method, "方法不能为空");
        McpTool mcpTool = method.getAnnotation(McpTool.class);
        Tool tool = method.getAnnotation(Tool.class);
        // 为了保证0侵入, 由于toolName是唯一的所以, 每次注册tool, 都要验证一下有没有相同的toolName, 然后修改
        if (mcpTool != null && StringUtils.hasText(mcpTool.name())) {
            presentToolName = mcpTool.name();
        } else if (tool != null && StringUtils.hasText(tool.name())) {
            presentToolName = tool.name();
        } else {
            presentToolName = className + "_" + method.getName();
        }

        return presentToolName;
    }

    public String getToolDescription(Method toolMethod, List<AbstractDesParser> desParserList) {
        return this.starter.runDesParse(desParserList, toolMethod, toolMethod.getDeclaringClass());
    }

    /**
     * 检查一个参数 是否必须，默认必须
     * @param method 方法
     * @param index 参数索引
     * @return
     */
    private boolean isMethodParameterRequired(List<AbstractParamParser> parserList, Method method, int index) {

        return starter.runParamRequiredParse(parserList, method, method.getDeclaringClass(), index);
    }


    private String getInputSchema(List<AbstractParamParser> paramParserList, Method method, JsonSchemaGenerator.SchemaOption... schemaOptions) {
        ObjectNode schema = JsonParser.getObjectMapper().createObjectNode();
        schema.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier());
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        List<String> required = new ArrayList<>();

        for(int i = 0; i < method.getParameterCount(); ++i) {
            String parameterName = method.getParameters()[i].getName();
            Type parameterType = method.getGenericParameterTypes()[i];

            // 因为需要做自动注入，所以需要排除掉这几个类型
            if (parameterType instanceof Class<?> parameterClass) {
                // 这个保留, 兼容springai
                if (ClassUtils.isAssignable(parameterClass, ToolContext.class)) {
                    continue;
                } else if (ClassUtils.isAssignable(parameterClass, McpAsyncServerExchange.class)) {
                    continue;
                } else if (ClassUtils.isAssignable(parameterClass, McpSyncServerExchange.class)) {
                    continue;
                }

                else if (ClassUtils.isAssignable(parameterClass, McpLogger.class)) {
                    continue;
                } else if (ClassUtils.isAssignable(parameterClass, McpLoggerFactory.class)) {
                    continue;
                } else if (ClassUtils.isAssignable(parameterClass, McpElicitation.class)) {
                    continue;
                } else if (ClassUtils.isAssignable(parameterClass, McpElicitationFactory.class)) {
                    continue;
                }  else if (ClassUtils.isAssignable(parameterClass, McpSampling.class)) {
                    continue;
                } else if (ClassUtils.isAssignable(parameterClass, McpSamplingFactory.class)) {
                    continue;
                } else if (ClassUtils.isAssignable(parameterClass, McpRoot.class)) {
                    continue;
                } else if (ClassUtils.isAssignable(parameterClass, McpRootFactory.class)) {
                    continue;
                }


            }

            // 这个应该是取交集 什么意思 举个例子 比如swagger3注解 解析出来 id和name都是不必须的 但是mvc解析出来都是必须的
            // 每一个参数都是一个链 mvc优先级大于 id 所以 两个都是必须的
            // 如果是所有参数都是一个链 会出现问题 导致控制的不够精细，如果我的参数列表就有一个参数配置了mvc的注解 其他的没有配置
            // 这种情况要不要传递给下一个处理对象呢？ 如果放任 现在配置了mvc注解的参数就会出现巨大问题 如果不管 那其他的参数就会出现问题
            // 理论上可以解决，比如把未处理的参数传递下去，但没有必要徒增工作量
            if (isMethodParameterRequired(paramParserList , method, i)) {
                required.add(parameterName);
            }

            // 构造参数对象 节点
            ObjectNode parameterNode = TOOL_SCHEMA_GENERATOR.generateSchema(parameterType);


            String parameterDescription = getMethodParameterDescription(paramParserList, method, i);


            // 由于对象类型比如Dto dto 它可以在类上写des, 也可以在对应参数上写des, 则是和参数上的des会对这个 类上的des进行覆盖并提醒
            if (StringUtils.hasText(parameterDescription)) {
                if (parameterNode.get("description") != null && StringUtils.hasText(parameterNode.get("description").asString())) {
                    log.warn("Please note the conflicting descriptions on the {} method", CommonUtil.getFullyQualifiedName(method));
                }

                parameterNode.put("description", parameterDescription);
            }

            properties.set(parameterName, parameterNode);
        }

        ArrayNode requiredArray = schema.putArray("required");
        Objects.requireNonNull(requiredArray);


        required.forEach(requiredArray::add);
        processSchemaOptions(schemaOptions, schema);
        return schema.toPrettyString();
    }


    private String getMethodParameterDescription(List<AbstractParamParser> parserList, Method method, int index) {
        return starter.runParamDesParse(parserList, method, method.getDeclaringClass(), index);
    }





    private void processSchemaOptions(JsonSchemaGenerator.SchemaOption[] schemaOptions, ObjectNode schema) {
        if (Stream.of(schemaOptions).noneMatch((option) -> {
            return option == JsonSchemaGenerator.SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT;
        })) {
            schema.put("additionalProperties", false);
        }

        if (Stream.of(schemaOptions).anyMatch((option) -> {
            return option == JsonSchemaGenerator.SchemaOption.UPPER_CASE_TYPE_VALUES;
        })) {
            convertTypeValuesToUpperCase(schema);
        }

    }


    public void convertTypeValuesToUpperCase(ObjectNode node) {
        if (node.isObject()) {
            // Jackson 3 迁移：fields() → properties()（返回 Set 而非 Iterator）
            node.properties().forEach((entry) -> {
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    convertTypeValuesToUpperCase((ObjectNode)value);
                } else if (value.isArray()) {
                    // Jackson 3 迁移：elements() → values()
                    value.values().forEach((element) -> {
                        if (element.isObject() || element.isArray()) {
                            convertTypeValuesToUpperCase((ObjectNode)element);
                        }

                    });
                } else if (value.isString() && "type".equals(entry.getKey())) {
                    // Jackson 3：isTextual() → isString()，asText() → asString()
                    String oldValue = node.get("type").asString();
                    node.put("type", oldValue.toUpperCase());
                }

            });
        } else if (node.isArray()) {
            node.values().forEach((element) -> {
                if (element.isObject() || element.isArray()) {
                    convertTypeValuesToUpperCase((ObjectNode)element);
                }

            });
        }

    }


    /**
     * 构建 Tool 的 inputSchema。
     * <p>
     * 原注释曾质疑「为何 inputSchema 与 outputSchema 使用两种数据结构」——MCP SDK 2.0
     * 已把 {@code Tool.inputSchema} 由 {@code McpSchema.JsonSchema} 统一为
     * {@code Map<String, Object>}（对应 2025-11-25 协议放宽 schema 为任意 JSON Schema
     * 2020-12 关键字），两者数据结构自此一致。
     * <p>
     * 同时 Spring AI 2.0 移除了 {@code ModelOptionsUtils} 的 JSON 转换方法，故改用本项目
     * {@link JsonParser}。
     */
    public Map<String, Object> buildToolInputSchema(String inputSchemaString) {
        return JsonParser.fromJson(inputSchemaString, new TypeReference<Map<String, Object>>() {
        });
    }

    public Map<String, Object> buildToolOutputSchema(Method method) {
        Assert.notNull(method, "方法不能为空");

        try {
            // 使用更精确的类型解析方式，尽量保留泛型信息
            Type returnType = getEnhancedReturnType(method);
            ObjectNode jsonNodes = MCP_SCHEMA_GENERATOR.generateSchema(returnType);

            // 将 ObjectNode 转换为 Map
            Map<String, Object> schemaMap = GenSchemaUtils.objectNodeToMap(jsonNodes);

            // 确保返回的 schema 是 object 类型，如果不是则进行包装
            return ensureObjectSchema(schemaMap, method);

        } catch (Exception e) {
            log.warn("生成输出 Schema 时出现异常，方法: {}, 错误: {}",
                    CommonUtil.getFullyQualifiedName(method), e.getMessage(), e);

            // 返回一个基本的 object schema 作为降级方案
            return createFallbackObjectSchema();
        }
    }

    /**
     * 获取增强的返回类型，尽量保留泛型信息
     */
    private Type getEnhancedReturnType(Method method) {
        try {
            // 首先尝试使用 ResolvableType 来获取更准确的类型信息
            ResolvableType resolvableType = ResolvableType.forMethodReturnType(method);
            if (resolvableType.hasGenerics()) {
                // 如果有泛型信息，尝试获取完整的类型
                return resolvableType.getType();
            }
        } catch (Exception e) {
            log.debug("使用 ResolvableType 解析返回类型失败，降级使用原始方式: {}", e.getMessage());
        }

        // 降级使用原来的方式
        return method.getGenericReturnType();
    }

    /**
     * 确保返回的 schema 是 object 类型
     * 如果原始 schema 不是 object 类型，则将其包装成 object
     */
    private Map<String, Object> ensureObjectSchema(Map<String, Object> originalSchema, Method method) {
        Object typeValue = originalSchema.get("type");

        // 如果已经是 object 类型，直接返回
        if ("object".equals(typeValue)) {
            return originalSchema;
        }

        // 如果不是 object 类型，进行包装
        log.debug("方法 {} 的返回类型不是 object，进行包装。原始类型: {}",
                CommonUtil.getFullyQualifiedName(method), typeValue);

        Map<String, Object> wrappedSchema = new HashMap<>();
        wrappedSchema.put("type", "object");

        // 创建 properties 对象
        Map<String, Object> properties = new HashMap<>();
        properties.put("result", originalSchema);
        wrappedSchema.put("properties", properties);

        // 设置 required 字段
        List<String> required = new ArrayList<>();
        required.add("result");
        wrappedSchema.put("required", required);

        // 添加描述信息
        String description = String.format("包装的返回结果，原始类型: %s", typeValue);
        wrappedSchema.put("description", description);

        return wrappedSchema;
    }

    /**
     * 创建降级的 object schema
     */
    private Map<String, Object> createFallbackObjectSchema() {
        Map<String, Object> fallbackSchema = new HashMap<>();
        fallbackSchema.put("type", "object");
        fallbackSchema.put("description", "通用返回对象 schema（降级方案）");

        Map<String, Object> properties = new HashMap<>();
        fallbackSchema.put("properties", properties);

        return fallbackSchema;
    }
}
