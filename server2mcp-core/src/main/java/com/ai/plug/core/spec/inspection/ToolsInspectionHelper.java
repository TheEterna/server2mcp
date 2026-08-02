package com.ai.plug.core.spec.inspection;

import com.ai.plug.core.annotation.McpTool;
import com.ai.plug.core.context.tool.IToolContext;
import com.ai.plug.core.context.tool.ToolContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool enumeration helper — dumps every tool registered in an
 * {@link IToolContext} along with its {@link McpTool} annotation state
 * (name, title, description, hints, ttlMs, cacheScope, resultType).
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var report = ToolsInspectionHelper.inspect(toolContext);
 *   log.info("Tools registered: {}", report);
 * }</pre>
 *
 * <p>Reflection access is read-only — the helper never mutates the tool
 * context. Designed for startup-time logging / dev-mode diagnostics.
 */
public final class ToolsInspectionHelper {

    private ToolsInspectionHelper() {
    }

    /**
     * Inspect all tools in the context. Returns a list of name + flag maps
     * (one per tool), suitable for logging or JSON serialization.
     */
    public static List<Map<String, Object>> inspect(IToolContext toolContext) {
        if (toolContext == null) {
            return List.of();
        }
        List<Map<String, Object>> report = new ArrayList<>();
        for (Map.Entry<String, ToolContext.ToolRegisterDefinition> entry
            : toolContext.getRawTools().entrySet()) {
            report.add(buildEntry(entry.getKey(), entry.getValue()));
        }
        return report;
    }

    private static Map<String, Object> buildEntry(String name,
                                                  ToolContext.ToolRegisterDefinition def) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        Method m = def == null ? null : (Method) extractMethod(def);
        if (m == null) {
            return entry;
        }
        McpTool ann = m.getAnnotation(McpTool.class);
        if (ann == null) {
            return entry;
        }
        // Annotation field map — only the fields most useful for diagnostics
        entry.put("title", ann.title());
        entry.put("description", ann.description());
        entry.put("resultType", ann.resultType());
        entry.put("ttlMs", ann.ttlMs());
        entry.put("cacheScope", ann.cacheScope());
        entry.put("cacheWrapperKey", ann.cacheWrapperKey());
        entry.put("readOnlyHint", ann.readOnlyHint());
        entry.put("destructiveHint", ann.destructiveHint());
        entry.put("idempotentHint", ann.idempotentHint());
        entry.put("openWorldHint", ann.openWorldHint());
        entry.put("returnDirect", ann.returnDirect());
        entry.put("listChanged", ann.listChanged());
        return entry;
    }

    /**
     * Extract the underlying {@link Method} from a {@link ToolContext.ToolRegisterDefinition}.
     * Reflective access since the registration class lives in core.context.tool.
     */
    private static Object extractMethod(ToolContext.ToolRegisterDefinition def) {
        try {
            var m = def.getClass().getMethod("getMethod");
            return m.invoke(def);
        }
        catch (Exception ex) {
            return null;
        }
    }
}