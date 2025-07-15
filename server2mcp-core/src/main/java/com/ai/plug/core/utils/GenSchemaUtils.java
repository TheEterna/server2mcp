package com.ai.plug.core.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger15.SwaggerOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import com.ai.plug.core.annotation.McpArg;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.ai.util.json.schema.SpringAiSchemaModule;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author han
 * @time 2025/7/7 11:15
 */

public class GenSchemaUtils {
    public final static SchemaGenerator TOOL_SCHEMA_GENERATOR;
    static {
        Module springAiSchemaModule = new SpringAiSchemaModule();
        Module mcpSchemaModule = new McpSchemaModule();
        Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
        Module swagger3Module = new Swagger2Module();
        Module swagger2Module = new SwaggerModule();
        JavaxValidationModule javaxValidationModule = new JavaxValidationModule(JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS);
        Module jakartaValidationModule = new JakartaValidationModule(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS);

        // 基本就很少用了，因为一般都是接口作为工具
        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder =
                (new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON))
                        .with(springAiSchemaModule)
                        .with(mcpSchemaModule)
                        .with(jacksonModule)
                        .with(swagger2Module)
                        .with(swagger3Module)
                        .with(javaxValidationModule)
                        .with(jakartaValidationModule)
                        .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                        .with(Option.PLAIN_DEFINITION_KEYS)

                ;

        SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.without(Option.SCHEMA_VERSION_INDICATOR).build();
        TOOL_SCHEMA_GENERATOR = new SchemaGenerator(subtypeSchemaGeneratorConfig);
    }

    public final static SchemaGenerator MCP_SCHEMA_GENERATOR;
    static {
        Module mcpSchemaModule = new McpSchemaModule();
        Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
        Module swagger3Module = new Swagger2Module();
        Module swagger2Module = new SwaggerModule(SwaggerOption.IGNORING_HIDDEN_PROPERTIES);
        JavaxValidationModule javaxValidationModule = new JavaxValidationModule(JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS);
        Module jakartaValidationModule = new JakartaValidationModule(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS);

        // 基本就很少用了，因为一般都是接口作为工具
        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder =
                (new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON))
                        .with(mcpSchemaModule)
                        .with(jacksonModule)
                        .with(swagger2Module)
                        .with(swagger3Module)
                        .with(javaxValidationModule)
                        .with(jakartaValidationModule)
                        .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                        .with(Option.PLAIN_DEFINITION_KEYS);

        SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.without(Option.SCHEMA_VERSION_INDICATOR).build();
        MCP_SCHEMA_GENERATOR = new SchemaGenerator(subtypeSchemaGeneratorConfig);
    }

//    public static Map<String, Object> objectNodeToMap(ObjectNode objectNode) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("type", objectNode.get("type"));
//        map.put("properties", objectNode.get("properties"));
//        map.put("required", objectNode.get("required"));
//        return map;
//    }
    public static Map<String, Object> objectNodeToMap(ObjectNode objectNode) {
        Map<String, Object> resultMap = new HashMap<>();
        // 遍历 ObjectNode 的所有字段（键值对）
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode valueNode = entry.getValue();
            // 递归转换字段值（处理嵌套对象、数组、基本类型等）
            Object value = convertJsonNode(valueNode);
            resultMap.put(key, value);
        }
        return resultMap;
    }
    private static Object convertJsonNode(JsonNode node) {
        if (node.isObject()) {
            // 如果是对象节点，递归转换为 Map
            return objectNodeToMap((ObjectNode) node);
        } else if (node.isArray()) {
            // 如果是数组节点，转换为 List
            ArrayNode arrayNode = (ArrayNode) node;
            List<Object> list = new ArrayList<>(arrayNode.size());
            for (JsonNode element : arrayNode) {
                list.add(convertJsonNode(element));
            }
            return list;
        } else if (node.isTextual()) {
            // 字符串类型
            return node.textValue();
        } else if (node.isNumber()) {
            // 数字类型（根据实际值返回 int、long、double 等）
            if (node.isInt()) {
                return node.intValue();
            } else if (node.isLong()) {
                return node.longValue();
            } else if (node.isDouble()) {
                return node.doubleValue();
            } else if (node.isFloat()) {
                return node.floatValue();
            } else if (node.isBigInteger()) {
                return node.bigIntegerValue();
            } else if (node.isBigDecimal()) {
                return node.decimalValue();
            }
            // 其他数字类型默认返回字符串（避免精度问题）
            return node.numberValue().toString();
        } else if (node.isBoolean()) {
            // 布尔类型
            return node.booleanValue();
        } else if (node.isNull()) {
            // null 值
            return null;
        } else if (node.isBinary()) {
            // 二进制数据（返回字节数组）
            try {
                return node.binaryValue();
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert binary node", e);
            }
        } else {
            // 其他类型（如 POJO、原始值等）默认返回字符串表示
            return node.toString();
        }
    }
    public static final class McpSchemaModule implements Module {
        private final boolean requiredByDefault;

        public McpSchemaModule(Option... options) {
            this.requiredByDefault = Stream.of(options).noneMatch((option) -> {
                return option == Option.PROPERTY_REQUIRED_FALSE_BY_DEFAULT;
            });
        }

        public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
            this.applyToConfigBuilder(builder.forFields());
        }

        private void applyToConfigBuilder(SchemaGeneratorConfigPart<FieldScope> configPart) {
            configPart.withDescriptionResolver(this::resolveDescription);

            configPart.withPropertyNameOverrideResolver(this::resolvePropertyNameOverride);

            configPart.withRequiredCheck(this::checkRequired);
        }



        @Nullable
        private String resolveDescription(MemberScope<?, ?> member) {
            McpArg McpArgAnnotation = (McpArg)member.getAnnotationConsideringFieldAndGetter(McpArg.class);
            return McpArgAnnotation != null && StringUtils.hasText(McpArgAnnotation.description()) ? McpArgAnnotation.description() : null;
        }

        private boolean checkRequired(MemberScope<?, ?> member) {
            McpArg McpArgAnnotation = (McpArg)member.getAnnotationConsideringFieldAndGetter(McpArg.class);
            if (McpArgAnnotation != null) {
                return McpArgAnnotation.required();
            } else {
                JsonProperty propertyAnnotation = (JsonProperty)member.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
                if (propertyAnnotation != null) {
                    return propertyAnnotation.required();
                } else {
                    Schema schemaAnnotation = (Schema)member.getAnnotationConsideringFieldAndGetter(Schema.class);
                    if (schemaAnnotation == null) {
                        Nullable nullableAnnotation = (Nullable)member.getAnnotationConsideringFieldAndGetter(Nullable.class);
                        return nullableAnnotation != null ? false : this.requiredByDefault;
                    } else {
                        return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED || schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO || schemaAnnotation.required();
                    }
                }
            }
        }

        private String resolvePropertyNameOverride(MemberScope<?, ?> member) {
            return this.getSchemaAnnotationValue(member, Schema::name, (name) -> {
                return !name.isEmpty();
            }).orElse(null);
        }

        private <T> Optional<T> getSchemaAnnotationValue(MemberScope<?, ?> member, Function<Schema, T> valueExtractor, Predicate<T> valueFilter) {
            if (member.isFakeContainerItemScope()) {
                return this.getArraySchemaAnnotation(member).map(ArraySchema::schema).map(valueExtractor).filter(valueFilter);
            } else {
                Schema annotation = (Schema)member.getAnnotationConsideringFieldAndGetter(Schema.class);
                return annotation != null ? Optional.of(annotation).map(valueExtractor).filter(valueFilter) : this.getArraySchemaAnnotation(member).map(ArraySchema::arraySchema).map(valueExtractor).filter(valueFilter);
            }
        }
        private Optional<ArraySchema> getArraySchemaAnnotation(MemberScope<?, ?> member) {
            return Optional.ofNullable((ArraySchema)member.getAnnotationConsideringFieldAndGetter(ArraySchema.class));
        }
        public static enum Option {
            PROPERTY_REQUIRED_FALSE_BY_DEFAULT;

            private Option() {
            }
        }
    }
}
