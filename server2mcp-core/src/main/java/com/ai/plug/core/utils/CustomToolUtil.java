package com.ai.plug.core.utils;

import com.ai.plug.core.provider.CustomToolCallResultConverter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ author 韩
 * time: 2025/5/14 0:28
 */

public class CustomToolUtil {


    public static List<ToolCallback> getToolCallbackListByName(List<ToolCallback> toolCallbackList, String name) {
        return toolCallbackList.stream().filter((toolCallback) -> {
            return toolCallback.getToolDefinition().name().equals(name);
        }).collect(Collectors.toList());



    }

    public static ToolCallResultConverter getToolCallResultConverter(Method method) {
        Assert.notNull(method, "method cannot be null");
        Tool tool = method.getAnnotation(Tool.class);
        if (tool == null) {
            return new CustomToolCallResultConverter();
        } else {
            Class<? extends ToolCallResultConverter> type = tool.resultConverter();

            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception var4) {
                throw new IllegalArgumentException("Failed to instantiate ToolCallResultConverter: " + String.valueOf(type), var4);
            }
        }
    }

}