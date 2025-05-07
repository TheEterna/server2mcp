package com.ai.plug.component.parser.param;

import com.ai.plug.component.parser.AbstractParser;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author: han
 * time: 2025/04/2025/4/1 23:18
 * des:
 */
public abstract class AbstractParamParser extends AbstractParser  {

    public abstract Boolean doParamRequiredParse(Method method, Class<?> toolClass, int index);


    public static Boolean doDefaultParamRequiredParse(Method method, Class<?> toolClass, int index) {
        Parameter parameter = method.getParameters()[index];

        // 默认检查是否必须操作
        Nullable nullableAnnotation = parameter.getAnnotation(Nullable.class);

        return nullableAnnotation == null;
    }


    public abstract String doParamDesParse(Method method, Class<?> toolClass, int index);


    public static String doDefaultParamDesParse(Method method, Class<?> toolClass, int index) {
        Parameter parameter = method.getParameters()[index];

        // 默认检查是否必须操作
        Nullable nullableAnnotation = parameter.getAnnotation(Nullable.class);

        return "";
    }


}
