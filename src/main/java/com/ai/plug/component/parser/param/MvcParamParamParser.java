package com.ai.plug.component.parser.param;

import com.ai.plug.component.config.PluginProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @ author 韩
 * time: 2025/4/30 2:55
 */
@Component
public class MvcParamParamParser extends AbstractParamParser {

    @Override
    protected PluginProperties.ParserType getName() {
        return null;
    }

    @Override
    public Boolean doParamRequiredParse(Method method, Class<?> toolClass, int index) {
        Parameter parameter = method.getParameters()[index];

        // springMVC 参数相关注解
        RequestParam requestParamAnnotation = parameter.getAnnotation(RequestParam.class);
        if (requestParamAnnotation != null) {
            return requestParamAnnotation.required();
        }
        RequestBody requestBodyAnnotation = parameter.getAnnotation(RequestBody.class);
        if (requestBodyAnnotation != null) {
            return requestBodyAnnotation.required();
        }
        PathVariable pathVariableAnnotation = parameter.getAnnotation(PathVariable.class);
        if (pathVariableAnnotation != null) {
            return pathVariableAnnotation.required();
        }
        RequestHeader requestHeaderAnnotation = parameter.getAnnotation(RequestHeader.class);
        if (requestHeaderAnnotation != null) {
            return requestHeaderAnnotation.required();
        }
        CookieValue cookieValueAnnotation = parameter.getAnnotation(CookieValue.class);
        if (cookieValueAnnotation != null) {
            return cookieValueAnnotation.required();
        }
        MatrixVariable matrixVariableAnnotation = parameter.getAnnotation(MatrixVariable.class);
        if (matrixVariableAnnotation != null) {
            return matrixVariableAnnotation.required();
        }
        RequestAttribute requestAttributeAnnotation = parameter.getAnnotation(RequestAttribute.class);
        if (requestAttributeAnnotation != null) {
            return requestAttributeAnnotation.required();
        }
        SessionAttribute sessionAttributeAnnotation = parameter.getAnnotation(SessionAttribute.class);
        if (sessionAttributeAnnotation != null) {
            return sessionAttributeAnnotation.required();
        }
        RequestPart requestPartAnnotation = parameter.getAnnotation(RequestPart.class);
        if (requestPartAnnotation != null) {
            return requestPartAnnotation.required();
        }

        // 如果没有设置自动绑定，那就是 只有初始化的值
        ModelAttribute modelAttributeAnnotation = parameter.getAnnotation(ModelAttribute.class);
        if (modelAttributeAnnotation != null) {
            return modelAttributeAnnotation.binding();
        }

        return null;
    }

    @Override
    public String doParamDesParse(Method method, Class<?> toolClass, int index) {
        // 由于mvc不提供注解功能，这里直接返回null，略过该处理器
        return null;
    }

}
