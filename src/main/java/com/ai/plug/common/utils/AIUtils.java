package com.ai.plug.common.utils;

import com.ai.plug.component.parser.des.AbstractDesParser;
import com.ai.plug.component.parser.param.AbstractParamParser;
import com.ai.plug.component.parser.starter.Starter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.ai.util.json.schema.SpringAiSchemaModule;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;


/**
 * @author: han
 * time: 2025/04/2025/4/1 15:40
 * des:
 */

@Slf4j
@Component
public class AIUtils {

    private static Starter starter;


    private static final SchemaGenerator SUBTYPE_SCHEMA_GENERATOR;
    static {
        Module springAiSchemaModule = new SpringAiSchemaModule();
        Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
        Module swagger3Module = new Swagger2Module();
        Module swagger2Module = new SwaggerModule();

        // 基本就很少用了，因为一般都是接口作为工具
        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder =
                (new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON))
                        .with(springAiSchemaModule)
                        .with(jacksonModule)
                        .with(swagger2Module)
                        .with(swagger3Module)
                        .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                        .with(Option.PLAIN_DEFINITION_KEYS);

        SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.without(Option.SCHEMA_VERSION_INDICATOR).build();
        SUBTYPE_SCHEMA_GENERATOR = new SchemaGenerator(subtypeSchemaGeneratorConfig);
    }

    /**
     * 构造Tool定义数据对象
     * @param toolMethod
     * @param desParserList
     * @return
     */
    public static ToolDefinition buildToolDefinition(Method toolMethod, List<AbstractDesParser> desParserList, List<AbstractParamParser> paramParserList, Starter starter) {
        Assert.notNull(toolMethod, "方法不能为空");
        AIUtils.starter = starter;



        return DefaultToolDefinition.builder().name(AIUtils.getToolName(toolMethod))
                .description(AIUtils.getToolDescription(toolMethod, desParserList))
                .inputSchema(AIUtils.getInputSchema(paramParserList, toolMethod)).build();

    }

    public static String getToolName(Method method) {

        String className = method.getDeclaringClass().getSimpleName();

        String presentToolName;
        // 把本来的操作作为备选
        Assert.notNull(method, "方法不能为空");
        Tool tool = method.getAnnotation(Tool.class);
        // 为了保证0侵入, 由于toolName是唯一的所以, 每次注册tool, 都要验证一下有没有相同的toolName, 然后修改
        if (tool == null) {
            presentToolName = className + "." + method.getName();
        } else {
            presentToolName = StringUtils.hasText(tool.name()) ? tool.name() :className + "." + method.getName();
        }

        return presentToolName;
    }

    public static String getToolDescription(Method toolMethod, List<AbstractDesParser> desParserList) {

        return starter.runDesParse(desParserList, toolMethod, toolMethod.getDeclaringClass());
    }

    /**
     * 检查一个参数 是否必须，默认必须
     * @param method
     * @param index
     * @return
     */
    private static boolean isMethodParameterRequired(List<AbstractParamParser> parserList, Method method, int index) {

        return starter.runParamRequiredParse(parserList, method, method.getDeclaringClass(), index);
    }


    public static String getInputSchema(List<AbstractParamParser> paramParserList, Method method, JsonSchemaGenerator.SchemaOption... schemaOptions) {
        ObjectNode schema = JsonParser.getObjectMapper().createObjectNode();
        schema.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier());
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        List<String> required = new ArrayList<>();

        for(int i = 0; i < method.getParameterCount(); ++i) {
            String parameterName = method.getParameters()[i].getName();
            Type parameterType = method.getGenericParameterTypes()[i];


            if (parameterType instanceof Class<?> parameterClass) {
                if (ClassUtils.isAssignable(parameterClass, ToolContext.class)) {
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
            ObjectNode parameterNode = SUBTYPE_SCHEMA_GENERATOR.generateSchema(parameterType);


            String parameterDescription = getMethodParameterDescription(paramParserList, method, i);


            // 由于对象类型比如Dto dto 它可以在类上写des, 也可以在对应参数上写des, 则是和参数上的des会对这个 类上的des进行覆盖并提醒
            if (StringUtils.hasText(parameterDescription)) {
                if (parameterNode.get("description") != null && StringUtils.hasText(parameterNode.get("description").asText())) {
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


    private static String getMethodParameterDescription(List<AbstractParamParser> parserList, Method method, int index) {
        return starter.runParamDesParse(parserList, method, method.getDeclaringClass(), index);
    }





    private static void processSchemaOptions(JsonSchemaGenerator.SchemaOption[] schemaOptions, ObjectNode schema) {
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
    public static void convertTypeValuesToUpperCase(ObjectNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining((entry) -> {
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    convertTypeValuesToUpperCase((ObjectNode)value);
                } else if (value.isArray()) {
                    value.elements().forEachRemaining((element) -> {
                        if (element.isObject() || element.isArray()) {
                            convertTypeValuesToUpperCase((ObjectNode)element);
                        }

                    });
                } else if (value.isTextual() && entry.getKey().equals("type")) {
                    String oldValue = node.get("type").asText();
                    node.put("type", oldValue.toUpperCase());
                }

            });
        } else if (node.isArray()) {
            node.elements().forEachRemaining((element) -> {
                if (element.isObject() || element.isArray()) {
                    convertTypeValuesToUpperCase((ObjectNode)element);
                }

            });
        }

    }



}
