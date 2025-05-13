package com.ai.plug.common.utils;

import org.springframework.ai.tool.ToolCallback;

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
}