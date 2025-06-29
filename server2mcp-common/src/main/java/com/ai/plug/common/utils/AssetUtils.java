package com.ai.plug.common.utils;

import org.slf4j.Logger;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author han
 * @time 2025/6/30 2:46
 */

public class AssetUtils {


    public static boolean isFunctionalType(Method toolMethod, Logger log) {
        boolean isFunction = ClassUtils.isAssignable(Function.class, toolMethod.getReturnType())
                || ClassUtils.isAssignable(Supplier.class, toolMethod.getReturnType())
                || ClassUtils.isAssignable(Consumer.class, toolMethod.getReturnType());

        if (isFunction) {
            log.warn("Method {} is a McpTool but returns a functional type. "
                    + "This is not supported and the method will be ignored.", toolMethod.getName());
        }

        return isFunction;
    }

}
