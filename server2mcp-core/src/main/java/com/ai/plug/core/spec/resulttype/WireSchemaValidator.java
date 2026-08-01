package com.ai.plug.core.spec.resulttype;

import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validator for wire-layer metadata in {@link McpSchema.CallToolResult} /
 * {@link McpSchema.ListToolsResult} / etc. Catches missing or malformed
 * entries that would cause downstream clients to choke on our 2026-07-28
 * wire output.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   WireSchemaValidator.Report report = WireSchemaValidator.validate(result);
 *   if (!report.isOk()) {
 *       log.warn("Wire schema problems: {}", report);
 *   }
 * }</pre>
 *
 * <p>This is a <b>development-time</b> helper, not a hot-path check. Use it
 * in tests, in /api/mcp/_validate debug endpoints, or in dev-time
 * configuration audits. Don't invoke per-call in production.
 */
public final class WireSchemaValidator {

    /** Required meta keys for a well-formed CallToolResult. */
    public static final Set<String> REQUIRED_META_KEYS = Set.of("resultType");

    /** Optional meta keys (TTL / cache hint). Missing is fine. */
    public static final Set<String> OPTIONAL_META_KEYS = Set.of(
        "ttlMs", "cacheScope", "cacheWrapperKey", "nextCursor", "totalItems",
        "traceparent", "tracestate", "baggage", "inputRequests", "requestState",
        "taskHandle"
    );

    private WireSchemaValidator() {
    }

    /**
     * Validate a {@link McpSchema.CallToolResult} and return a report.
     */
    public static Report validate(McpSchema.CallToolResult result) {
        return validateMeta(result.meta(), "CallToolResult");
    }

    /**
     * Validate a {@link McpSchema.ListToolsResult} (or sibling List*Result)
     * and return a report.
     */
    public static Report validateListResult(McpSchema.ListToolsResult result) {
        return validateMeta(result.meta(), "ListToolsResult");
    }

    /**
     * Validate a wire meta map directly.
     *
     * @param sourceLabel "CallToolResult" / "ListToolsResult" / etc — used in
     *                    the report's error messages
     */
    public static Report validateMeta(@Nullable Map<String, Object> meta, String sourceLabel) {
        Report report = new Report(sourceLabel);
        if (meta == null) {
            report.add("meta is null (no wire-layer hints at all)");
            return report;
        }
        // Required keys
        for (String required : REQUIRED_META_KEYS) {
            if (!meta.containsKey(required)) {
                report.add("missing required key: " + required);
            }
        }
        // resultType must be one of the valid literals
        Object rt = meta.get("resultType");
        if (rt != null) {
            if (!(rt instanceof String)) {
                report.add("resultType is not a String: " + rt.getClass().getName());
            }
            else {
                String rtStr = (String) rt;
                if (!com.ai.plug.core.spec.resulttype.ResultTypeConvention.COMPLETE.equals(rtStr)
                    && !com.ai.plug.core.spec.resulttype.ResultTypeConvention.INPUT_REQUIRED.equals(rtStr)) {
                    report.add("resultType has unknown value: " + rtStr);
                }
            }
        }
        // ttlMs must be a non-negative number when present
        Object ttl = meta.get("ttlMs");
        if (ttl != null) {
            if (!(ttl instanceof Number)) {
                report.add("ttlMs is not a Number: " + ttl.getClass().getName());
            }
            else if (((Number) ttl).longValue() < 0) {
                report.add("ttlMs must be >= 0, got: " + ttl);
            }
        }
        // cacheScope must be a known literal
        Object scope = meta.get("cacheScope");
        if (scope != null) {
            if (!com.ai.plug.core.spec.cacheable.CacheHints.CACHE_SCOPE_PUBLIC.equals(scope)
                && !com.ai.plug.core.spec.cacheable.CacheHints.CACHE_SCOPE_PRIVATE.equals(scope)) {
                report.add("cacheScope has unknown value: " + scope);
            }
        }
        return report;
    }

    /**
     * Validation result — accumulates issues; isOk() returns true when none.
     */
    public static final class Report {
        private final String source;
        private final java.util.List<String> issues = new java.util.ArrayList<>();

        Report(String source) { this.source = source; }

        public String source() { return source; }
        public List<String> issues() { return List.copyOf(issues); }
        public boolean isOk() { return issues.isEmpty(); }

        void add(String issue) { issues.add(issue); }

        @Override
        public String toString() {
            if (isOk()) return "Report[" + source + ": OK]";
            return "Report[" + source + ": " + issues.size() + " issue(s)]\n  - "
                + String.join("\n  - ", issues);
        }
    }
}