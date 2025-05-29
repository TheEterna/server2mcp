package com.ai.plug.component.provider;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

/**
 * @ author 韩
 * time: 2025/5/22 16:33
 */

public interface ResourcesCallback {
    ToolDefinition getToolDefinition();

    default ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    String call(String toolInput);

    default String call(String toolInput, @Nullable ToolContext tooContext) {
        if (tooContext != null && !tooContext.getContext().isEmpty()) {
            throw new UnsupportedOperationException("Tool context is not supported!");
        } else {
            return this.call(toolInput);
        }
    }
}
