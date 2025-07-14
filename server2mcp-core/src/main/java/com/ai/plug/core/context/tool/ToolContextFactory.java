package com.ai.plug.core.context.tool;

/**
 * @author han
 * @time 2025/7/14 16:01
 */

public class ToolContextFactory {
    /**
     * 创建 Tool 容器
     */
    public static IToolContext createToolContext() {
        return new ToolContext();
    }
}
