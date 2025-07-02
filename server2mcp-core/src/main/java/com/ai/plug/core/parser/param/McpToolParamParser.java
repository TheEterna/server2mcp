package com.ai.plug.core.parser.param;

import com.logaritex.mcp.annotation.McpArg;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author han
 * @time 2025/6/30 3:49
 */

public class McpToolParamParser extends AbstractParamParser {
    @Override
    public Boolean doParamRequiredParse(Method method, Class<?> toolClass, int index) {

        Parameter parameter = method.getParameters()[index];
        McpArg toolParamAnnotation = parameter.getAnnotation(McpArg.class);
        if (toolParamAnnotation != null) {
            return toolParamAnnotation.required();
        }

        return null;
    }

    @Override
    public String doParamDesParse(Method method, Class<?> toolClass, int index) {

        Parameter parameter = method.getParameters()[index];

        McpArg toolParamAnnotation = parameter.getAnnotation(McpArg.class);
        if (toolParamAnnotation != null && StringUtils.hasText(toolParamAnnotation.description())) {
            return toolParamAnnotation.description();
        }

        return null;
    }
}