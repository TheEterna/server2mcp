package com.ai.plug.common.utils;

import com.ai.plug.component.parser.des.AbstractDesParser;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.ParsingUtils;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.ai.util.json.schema.SpringAiSchemaModule;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;


/**
 * @author: han
 * time: 2025/04/2025/4/1 15:40
 * des: AI操作工具类
 */

@Slf4j
public class AIUtils {



    private static final boolean PROPERTY_REQUIRED_BY_DEFAULT = true;
    private static final SchemaGenerator TYPE_SCHEMA_GENERATOR;
    private static final SchemaGenerator SUBTYPE_SCHEMA_GENERATOR;
    SchemaGeneratorConfig subtypeSchemaGeneratorConfig;
    static {
        Module jacksonModule = new JacksonModule(new JacksonOption[]{JacksonOption.RESPECT_JSONPROPERTY_REQUIRED});
        Module openApiModule = new Swagger2Module();
        Module springAiSchemaModule = new SpringAiSchemaModule(new SpringAiSchemaModule.Option[0]);
        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder = (new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)).with(jacksonModule).with(openApiModule).with(springAiSchemaModule).with(Option.EXTRA_OPEN_API_FORMAT_VALUES, new Option[0]).with(Option.PLAIN_DEFINITION_KEYS, new Option[0]);
        SchemaGeneratorConfig typeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.build();
        TYPE_SCHEMA_GENERATOR = new SchemaGenerator(typeSchemaGeneratorConfig);
        SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.without(Option.SCHEMA_VERSION_INDICATOR, new Option[0]).build();
        SUBTYPE_SCHEMA_GENERATOR = new SchemaGenerator(subtypeSchemaGeneratorConfig);
    }

    public static String getToolName(Method method) {


        // 把本来的操作作为备选
        Assert.notNull(method, "方法不能为空");
        Tool tool = (Tool)method.getAnnotation(Tool.class);
        if (tool == null) {
            return method.getName();
        } else {
            return StringUtils.hasText(tool.name()) ? tool.name() : method.getName();
        }
    }

    public static String getToolDescription(Method toolMethod, AbstractDesParser parserHandler) {


        String result = "";
        try {
            result += parserHandler.handleParse(toolMethod, toolMethod.getDeclaringClass());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }




        if (!result.isBlank()) {
            return result;
        }
        // 把本来的操作作为备选
        Assert.notNull(toolMethod, "方法不能为空");
        Tool tool = (Tool)toolMethod.getAnnotation(Tool.class);
        if (tool == null) {
            return ParsingUtils.reConcatenateCamelCase(toolMethod.getName(), " ");
        } else {
            return StringUtils.hasText(tool.description()) ? tool.description() : toolMethod.getName();
        }



    }



    public static String getInputSchema(Method toolMethod) {



        // 把本来的操作作为备选
        return generateForMethodInput(toolMethod, new JsonSchemaGenerator.SchemaOption[0]);
    }


    private static boolean isMethodParameterRequired(Method method, int index) {
        Parameter parameter = method.getParameters()[index];
        ToolParam toolParamAnnotation = (ToolParam)parameter.getAnnotation(ToolParam.class);
        if (toolParamAnnotation != null) {
            return toolParamAnnotation.required();
        }


        JsonProperty propertyAnnotation = (JsonProperty)parameter.getAnnotation(JsonProperty.class);
        if (propertyAnnotation != null) {
            return propertyAnnotation.required();
        }

        Schema schemaAnnotation = (Schema)parameter.getAnnotation(Schema.class);
        if (schemaAnnotation == null) {
            Nullable nullableAnnotation = (Nullable)parameter.getAnnotation(Nullable.class);
            return nullableAnnotation == null;
        } else {
            return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED || schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO || schemaAnnotation.required();
        }


    }


    public static String generateForMethodInput(Method method, JsonSchemaGenerator.SchemaOption... schemaOptions) {
        ObjectNode schema = JsonParser.getObjectMapper().createObjectNode();
        schema.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier());
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        List<String> required = new ArrayList();

        for(int i = 0; i < method.getParameterCount(); ++i) {
            String parameterName = method.getParameters()[i].getName();
            Type parameterType = method.getGenericParameterTypes()[i];
            if (parameterType instanceof Class<?> parameterClass) {
                if (ClassUtils.isAssignable(parameterClass, ToolContext.class)) {
                    continue;
                }
            }

            if (isMethodParameterRequired(method, i)) {
                required.add(parameterName);
            }

            ObjectNode parameterNode = SUBTYPE_SCHEMA_GENERATOR.generateSchema(parameterType, new Type[0]);
            String parameterDescription = getMethodParameterDescription(method, i);
            if (StringUtils.hasText(parameterDescription)) {
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


    private static String getMethodParameterDescription(Method method, int index) {


        Parameter parameter = method.getParameters()[index];
        String parameterName = parameter.getName();

        // toolParam 有数据当然用ToolParam里的
        ToolParam toolParamAnnotation = (ToolParam)parameter.getAnnotation(ToolParam.class);
        if (toolParamAnnotation != null && StringUtils.hasText(toolParamAnnotation.description())) {
            return toolParamAnnotation.description();
        }

        JsonPropertyDescription jacksonAnnotation = (JsonPropertyDescription)parameter.getAnnotation(JsonPropertyDescription.class);
        if (jacksonAnnotation != null && StringUtils.hasText(jacksonAnnotation.value())) {
            return jacksonAnnotation.value();
        }
        Schema schemaAnnotation = (Schema)parameter.getAnnotation(Schema.class);
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


    /**
     *  构造Tool定义数据对象
     * @param toolMethod
     * @param parserHandler
     * @return
     */
    public static ToolDefinition buildToolDefinition(Method toolMethod, AbstractDesParser parserHandler) {


        return DefaultToolDefinition.builder().name(AIUtils.getToolName(toolMethod))
                .description(AIUtils.getToolDescription(toolMethod, parserHandler))
                .inputSchema(AIUtils.getInputSchema(toolMethod)).build();

    }



    /**
     * 提取参数数据 所需要函数
     * */
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
                JsonNode value = (JsonNode)entry.getValue();
                if (value.isObject()) {
                    convertTypeValuesToUpperCase((ObjectNode)value);
                } else if (value.isArray()) {
                    value.elements().forEachRemaining((element) -> {
                        if (element.isObject() || element.isArray()) {
                            convertTypeValuesToUpperCase((ObjectNode)element);
                        }

                    });
                } else if (value.isTextual() && ((String)entry.getKey()).equals("type")) {
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
