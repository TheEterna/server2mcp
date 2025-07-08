package com.ai.plug.core.parser.tool.param;

import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author 韩
 * time: 2025/4/30 2:56
 */
public class ToolParamParser extends AbstractParamParser {
    @Override
    public Boolean doParamRequiredParse(Method method, Class<?> toolClass, int index) {

        Parameter parameter = method.getParameters()[index];
        ToolParam toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
        if (toolParamAnnotation != null) {
            return toolParamAnnotation.required();
        }

        return null;
    }

    @Override
    public String doParamDesParse(Method method, Class<?> toolClass, int index) {

        Parameter parameter = method.getParameters()[index];

        ToolParam toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
        if (toolParamAnnotation != null && StringUtils.hasText(toolParamAnnotation.description())) {
            return toolParamAnnotation.description();
        }

        return null;
    }
}