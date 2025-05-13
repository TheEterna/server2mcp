package com.ai.plug.component.parser.param;

import com.ai.plug.component.config.PluginProperties;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @ author 韩
 * time: 2025/4/30 2:56
 */
@Component
public class Swagger2ParamParamParser extends AbstractParamParser {


    @Override
    protected PluginProperties.ParserType getName() {
        return null;
    }

    @Override
    public Boolean doParamRequiredParse(Method method, Class<?> toolClass, int index) {
        Parameter parameter = method.getParameters()[index];

        ApiParam apiParamAnnotation = parameter.getAnnotation(ApiParam.class);
        if (apiParamAnnotation != null) {
            return apiParamAnnotation.required();
        }
        ApiImplicitParam apiImplicitParamAnnotation = method.getAnnotation(ApiImplicitParam.class);
        if (apiImplicitParamAnnotation != null && apiImplicitParamAnnotation.name().equals(parameter.getName())) {

            return apiImplicitParamAnnotation.required();
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
        return null;
    }

    @Override
    public String doParamDesParse(Method method, Class<?> toolClass, int index) {
        Parameter parameter = method.getParameters()[index];

        ApiParam apiParamAnnotation = parameter.getAnnotation(ApiParam.class);
        if (apiParamAnnotation != null && StringUtils.hasText(apiParamAnnotation.value())) {
            return apiParamAnnotation.value();
        }

        ApiImplicitParam apiImplicitParamAnnotation = method.getAnnotation(ApiImplicitParam.class);
        if (apiImplicitParamAnnotation != null
                && apiImplicitParamAnnotation.name().equals(parameter.getName())
                && StringUtils.hasText(apiImplicitParamAnnotation.value())) {
            return apiImplicitParamAnnotation.value();
        }
        ApiImplicitParams apiImplicitParamsAnnotation = method.getAnnotation(ApiImplicitParams.class);
        if (apiImplicitParamsAnnotation != null) {
            for (ApiImplicitParam itemApiImplicitParam : apiImplicitParamsAnnotation.value()) {
                // 如果在parameters里有 这个属性就返回
                if (itemApiImplicitParam.name().equals(parameter.getName())) {
                    if (StringUtils.hasText(itemApiImplicitParam.value())) {
                        return itemApiImplicitParam.value();
                    }
                    break;
                }
            }
        }





        return null;
    }
}