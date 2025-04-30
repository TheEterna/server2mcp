package com.ai.plug.component.parser.starter;

import com.ai.plug.component.parser.AbstractParser;
import com.ai.plug.component.parser.des.AbstractDesParser;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * @ author 韩
 * time: 2025/4/29 17:02
 */

public abstract class Starter {
    public abstract String runDesParse(List<AbstractDesParser> parserList, Method method, Class<?> toolClass);

    public Boolean runParamRequiredParse(Method method, int index) {
        Parameter parameter = method.getParameters()[index];
        Class<?> parameterType = parameter.getType();


        ToolParam toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
        if (toolParamAnnotation != null) {
            return toolParamAnnotation.required();
        }
        {
            // 这个是 映射的逻辑，它是首位，如果这个参数json都决定不映射，那就不映射
            JsonProperty propertyAnnotation = parameter.getAnnotation(JsonProperty.class);
            if (propertyAnnotation != null) {
                return propertyAnnotation.required();
            }
        }
        // 先看 swagger3 注解
        {
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
        }

        // swagger2 注解
        {
            ApiParam apiParamAnnotation = parameter.getAnnotation(ApiParam.class);
            if (apiParamAnnotation != null) {
                return apiParamAnnotation.required();
            }
            // 由于swagger还有方法配置模式，还需要看方法里的Parameters注解和分离的Parameter注解(超过两个会转化成Parameters注解)，代码从上到下，优先级递减
            ApiImplicitParam apiImplicitParamAnnotation = method.getAnnotation(ApiImplicitParam.class);
            if (apiImplicitParamAnnotation != null && apiImplicitParamAnnotation.name().equals(parameter.getName())) {

                return apiImplicitParamAnnotation.required();
            }
            // 需要增加处理对象逻辑
            if (parameter.getAnnotation(RequestBody.class) != null) {
                // 标志着这是一个list, map, dto
                if (parameterType.isInstance(List.class)) {

                }
            }

            ApiImplicitParams apiImplicitParamsAnnotation = method.getAnnotation(ApiImplicitParams.class);
            if (apiImplicitParamsAnnotation != null) {
                for (ApiImplicitParam itemApiImplicitParam : apiImplicitParamsAnnotation.value()) {
                    // 如果在parameters里有 这个属性就返回
                    if (itemApiImplicitParam.name().equals(parameter.getName())) {
                        return itemApiImplicitParam.required();
                    }
                }
            }




        }


        // 如果有标志可以为空，那就返回不是必需的
        {
            Nullable nullableAnnotation = parameter.getAnnotation(Nullable.class);
            return nullableAnnotation == null;
        }
    }

}
