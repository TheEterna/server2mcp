package com.ai.plug.core.parser.tool.des;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * @author 韩
 * time: 2025/5/16 10:27
 */

public class JacksonDesParser extends AbstractDesParser {

    @Override
    public String doDesParse(Method method, Class<?> toolClass) {
        JsonPropertyDescription jsonPropertyDescriptionAnnotation = method.getAnnotation(JsonPropertyDescription.class);

        if (jsonPropertyDescriptionAnnotation == null) {
            return null;
        }
        if (StringUtils.hasText(jsonPropertyDescriptionAnnotation.value())) {
            return jsonPropertyDescriptionAnnotation.value();
        }
        return null;

    }
}
