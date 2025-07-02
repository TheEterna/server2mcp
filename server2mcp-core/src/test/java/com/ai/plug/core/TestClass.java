package com.ai.plug.core;

import com.ai.plug.core.spec.utils.logging.McpLogger;
import org.springframework.util.ClassUtils;

/**
 * @author han
 * @time 2025/7/3 1:15
 */

public class TestClass {
    public static void main(String[] args) {
        System.out.println(ClassUtils.isAssignable(Object.class, McpLogger.class));
    }
}
