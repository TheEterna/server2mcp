package com.ai.plug.component.parser.param;

import com.ai.plug.component.config.PluginProperties;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @ author 韩
 * time: 2025/4/30 2:56
 */

@Component
public class Swagger3ParamParamParser extends AbstractParamParser {

    @Override
    protected PluginProperties.ParserType getName() {
        return null;
    }

    @Override
    public Boolean doParamRequiredParse(Method method, Class<?> toolClass, int index) {


        Parameter parameter = method.getParameters()[index];
        io.swagger.v3.oas.annotations.Parameter parameterAnnotation = parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
        if (parameterAnnotation != null) {
            return parameterAnnotation.required();
        }
        // 由于swagger还有方法配置模式，还需要看方法里的Parameters注解和分离的Parameter注解(超过两个会转化成Parameters注解)，代码从上到下，优先级递减
        io.swagger.v3.oas.annotations.Parameter methodParameterAnnotation = method.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
        if (methodParameterAnnotation != null && methodParameterAnnotation.name().equals(parameter.getName())) {
            return methodParameterAnnotation.required();
        }
        Parameters methodParametersAnnotation = method.getAnnotation(Parameters.class);
        if (methodParametersAnnotation != null) {
            for (io.swagger.v3.oas.annotations.Parameter itemParameter : methodParametersAnnotation.value()) {
                // 如果在parameters里有 这个属性就返回
                if (itemParameter.name().equals(parameter.getName())) {
                    return itemParameter.required();
                }
            }
        }

        Schema schemaAnnotation = parameter.getAnnotation(Schema.class);
        if (schemaAnnotation != null) {
            return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED
                    || schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO
                    || schemaAnnotation.required();

        }

        // 一个都没解析到就返回 null
        return null;
    }

    @Override
    public String doParamDesParse(Method method, Class<?> toolClass, int index) {
        return null;
    }
}