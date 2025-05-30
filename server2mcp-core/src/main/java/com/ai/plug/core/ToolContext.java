package com.ai.plug.core;

import com.ai.plug.core.annotation.ToolScan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: han
 * time: 2025/04/2025/4/11 02:33
 * des:
 */
@Component
@Slf4j
public class ToolContext {

    private static Map<String, ToolRegisterDefinition> rawTools = new HashMap<>();

    public ToolContext() {
    }

    public static Map<String, ToolRegisterDefinition> getRawTools() {
        return rawTools;
    }

    public static void addTool(String name, ToolRegisterDefinition tool) {
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
