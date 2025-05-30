package com.ai.plug.core.provider;

import org.springframework.ai.tool.StaticToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;

/**
 * @ author 韩
 * time: 2025/5/21 23:23
 */

public interface ResourceCallbackProvider {
    ToolCallback[] getToolCallbacks();



    static ToolCallbackProvider from(List<? extends ToolCallback> toolCallbacks) {
        return new StaticToolCallbackProvider(toolCallbacks);
    }

    static ToolCallbackProvider from(ToolCallback... toolCallbacks) {
        return new StaticToolCallbackProvider(toolCallbacks);
    }
}
