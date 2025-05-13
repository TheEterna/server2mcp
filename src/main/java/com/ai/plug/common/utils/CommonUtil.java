package com.ai.plug.common.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @ author 韩
 * time: 2025/5/7 22:59
 */

public class CommonUtil {

//    public static Boolean isOK

    public static String getFullyQualifiedName(Method method) {
        // 获取类的全限定名
        String className = method.getDeclaringClass().getName();

        // 获取方法名
        String methodName = method.getName();

        // 构建参数类型列表
        StringBuilder params = new StringBuilder();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            params.append(parameters[i].getType().getName());
            if (i < parameters.length - 1) {
                params.append(",");
            }
        }

        // 组合完整格式
        return className + "." + methodName + "(" + params + ")";
    }}
