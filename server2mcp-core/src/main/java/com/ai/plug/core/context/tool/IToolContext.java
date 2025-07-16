package com.ai.plug.core.context.tool;

import java.util.Map;

/**
 * @author han
 * @time 2025/7/14 11:34
 */

public interface IToolContext {
    /**
     * get raw tools
     */
    Map<String, ToolContext.ToolRegisterDefinition> getRawTools();

    /**
     * add tool
     */
    void addTool(String name, ToolContext.ToolRegisterDefinition tool);

}
