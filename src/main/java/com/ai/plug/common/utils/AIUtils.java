package com.ai.plug.common.utils;

import com.ai.plug.component.parser.AbstractParser;
import com.ai.plug.component.parser.des.AbstractDesParser;
import com.ai.plug.component.parser.param.AbstractParamParser;
import com.ai.plug.component.parser.starter.Starter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.ai.util.json.schema.SpringAiSchemaModule;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * @author: han
 * time: 2025/04/2025/4/1 15:40
 * des: AI操作工具类
 */

@Slf4j
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

        // 把本来的操作作为备选
        Assert.notNull(method, "方法不能为空");
        Tool tool = method.getAnnotation(Tool.class);
        if (tool == null) {
            return className + "." + method.getName();
        } else {
            return className + "." + (StringUtils.hasText(tool.name()) ? tool.name() : method.getName()) ;
        }
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


            String parameterDescription = getMethodParameterDescription(method, i);
            if (StringUtils.hasText(parameterDescription)) {
                parameterNode.put("description", parameterDescription);
            }

            properties.set(parameterName, parameterNode);
        }

        ArrayNode requiredArray = schema.putArray("required");
//        Objects.requireNonNull(requiredArray);


        required.forEach(requiredArray::add);
        processSchemaOptions(schemaOptions, schema);
        return schema.toPrettyString();
    }


    private static String getMethodParameterDescription(Method method, int index) {


        // todo 逻辑
        Parameter parameter = method.getParameters()[index];
        String parameterName = parameter.getName();

        // toolParam 有数据当然用ToolParam里的
        ToolParam toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
        if (toolParamAnnotation != null && StringUtils.hasText(toolParamAnnotation.description())) {
            return toolParamAnnotation.description();
        }

        JsonPropertyDescription jacksonAnnotation = parameter.getAnnotation(JsonPropertyDescription.class);
        if (jacksonAnnotation != null && StringUtils.hasText(jacksonAnnotation.value())) {
            return jacksonAnnotation.value();
        }
        Schema schemaAnnotation = parameter.getAnnotation(Schema.class);
        if (schemaAnnotation != null && StringUtils.hasText(schemaAnnotation.description())) {
            return schemaAnnotation.description();
        }



        String className = method.getDeclaringClass().getName().replace('.', '/') + ".java";
        File file = new File("src/main/java/"  + className);

        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(file);
            // 遍历所有方法声明
            for (MethodDeclaration methodDeclaration : compilationUnit.findAll(MethodDeclaration.class)) {
                if (methodDeclaration.getNameAsString().equals(method.getName())
                        && methodDeclaration.getParameters().size() == method.getParameterCount()
                        && methodDeclaration.hasJavaDocComment()
                ) {
                    // 检查参数类型是否匹配
                    boolean match = true;
                    // 对比每个参数
                    for (int i = 0; i < methodDeclaration.getParameters().size(); i++) {
                        String paramType = methodDeclaration.getParameters().get(i).getType().toString();
                        String actualParamType = method.getParameterTypes()[i].getSimpleName();
                        if (!paramType.equals(actualParamType)) {
                            match = false;
                            break;
                        }
                    }
                    // 如果不匹配， 就continue
                    if (!match) continue;

                    // 到这里了，说明找到了匹配方法，并有注释
                    Javadoc javadoc = methodDeclaration.getJavadoc().get();
                    // 带 tag的信息 例如@param
                    List<JavadocBlockTag> blockTags = javadoc.getBlockTags();
                    for (JavadocBlockTag item : blockTags) {
                        if (item.getName().equals(parameterName) && JavadocBlockTag.Type.PARAM == item.getType()) {
                            return item.getContent().getElements().get(0).toText();
                        }
                    }

                    return "暂不了解该参数意义, 我将返回其javadoc和方法体,供你了解 javadoc：" + methodDeclaration.getJavadoc() + "方法体：" + methodDeclaration.getBody().map(Object::toString).orElse("");
                }

                else {
                    if (methodDeclaration.getComment().isPresent()) {
                        return "暂不了解该参数意义, 我将返回其方法体和注释,供你了解 方法体：" +  methodDeclaration.getBody().map(Object::toString).orElse("") + "注释：" + methodDeclaration.getComment().get();
                    } else {
                        return "暂不了解该参数意义, 我将返回其方法体,供你了解" +  methodDeclaration.getBody().map(Object::toString).orElse("");
                    }
                }
            }


        } catch (FileNotFoundException e) {
            log.error("没找到该文件: " + "classPath" + ", 请检查");
        }

        return "抱歉,我无法向你提供该参数的描述,请根据上下文推测其意义";

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
