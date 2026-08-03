package com.ai.plug.core.spec.inspection;

import com.ai.plug.core.context.tool.IToolContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight companion to {@link ToolsInspectionHelper} for tasks.
 * Tasks are not registered through the standard tool context (they're
 * managed by custom MCP server code), so this is a stub: returning an
 * empty inspection list with a status note. When a tasks registry is
 * introduced, this can grow alongside it.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var report = TasksInspectionHelper.inspect(toolContext);
 *   log.info("Tasks: {}", report);
 * }</pre>
 */
public final class TasksInspectionHelper {

    private TasksInspectionHelper() {
    }

    /**
     * Return a placeholder tasks report. Today this is just an empty list
     * with a status note — the real task registry will be added in a
     * future Phase.
     */
    public static List<Map<String, Object>> inspect(IToolContext toolContext) {
        Map<String, Object> placeholder = new LinkedHashMap<>();
        placeholder.put("status", "tasks registry not yet exposed");
        placeholder.put("note", "see docs/mcp-2026-07-28-coverage.md (Phase 2 §3.2)");
        return List.of(placeholder);
    }
}