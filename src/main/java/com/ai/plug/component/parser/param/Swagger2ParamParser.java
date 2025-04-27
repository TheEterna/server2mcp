package com.ai.plug.component.parser.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.Module;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @ author 韩
 * time: 2025/4/23 4:04
 */

public class Swagger2ParamParser extends AbstractParamParser {


    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        applyToConfigBuilder(builder.forFields());
    }

    private void applyToConfigBuilder(SchemaGeneratorConfigPart<FieldScope> configPart) {
        configPart.withDescriptionResolver(this::resolveDescription);
        configPart.withRequiredCheck(this::checkRequired);
    }
    @Nullable
    private String resolveDescription(MemberScope<?, ?> member) {
        ToolParam toolParamAnnotation = (ToolParam)member.getAnnotationConsideringFieldAndGetter(ToolParam.class);
        return toolParamAnnotation != null && StringUtils.hasText(toolParamAnnotation.description()) ? toolParamAnnotation.description() : null;
    }

    private boolean checkRequired(MemberScope<?, ?> member) {
        ToolParam toolParamAnnotation = (ToolParam)member.getAnnotationConsideringFieldAndGetter(ToolParam.class);
        if (toolParamAnnotation != null) {
            return toolParamAnnotation.required();
        } else {
            JsonProperty propertyAnnotation = (JsonProperty)member.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
            if (propertyAnnotation != null) {
                return propertyAnnotation.required();
            } else {
                Schema schemaAnnotation = (Schema)member.getAnnotationConsideringFieldAndGetter(Schema.class);
                if (schemaAnnotation == null) {
                    Nullable nullableAnnotation = (Nullable)member.getAnnotationConsideringFieldAndGetter(Nullable.class);
                    return nullableAnnotation == null ;
                } else {
                    return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED || schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO || schemaAnnotation.required();
                }
            }
        }
    }
}
