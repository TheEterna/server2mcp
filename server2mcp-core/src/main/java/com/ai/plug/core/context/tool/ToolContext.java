package com.ai.plug.core.context.tool;

import com.ai.plug.core.annotation.ToolScan;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: han
 * time: 2025/04/2025/4/11 02:33
 * des:
 */
@Slf4j
public class ToolContext implements IToolContext{

    private static Map<String, ToolRegisterDefinition> rawTools = new ConcurrentHashMap<>();

    ToolContext() {
    }

    @Override
    public Map<String, ToolRegisterDefinition> getRawTools() {
        return Collections.unmodifiableMap(rawTools);
    }

    @Override
    public void addTool(String name, ToolRegisterDefinition tool) {
        rawTools.put(name, tool);
    }

    public static class ToolRegisterDefinition {
        ToolScan.ToolFilter[] includeFilters;
        ToolScan.ToolFilter[] excludeFilters;

        public ToolScan.ToolFilter[] getIncludeFilters() {
            return includeFilters;
        }

        public ToolScan.ToolFilter[] getExcludeFilters() {
            return excludeFilters;
        }

        public ToolRegisterDefinition(ToolScan.ToolFilter[] includeFilters, ToolScan.ToolFilter[] excludeFilters) {
            this.includeFilters = includeFilters;
            this.excludeFilters = excludeFilters;
        }
    }


}
