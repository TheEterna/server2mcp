package com.ai.plug.core.spec.resulttype;

import com.ai.plug.common.utils.JsonParser;
import com.ai.plug.core.spec.cacheable.CacheHints;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Wire-level JSON serializer for MCP results that carries protocol 2026-07-28
 * fields SDK 2.0 cannot express: {@code resultType}, {@code ttlMs},
 * {@code cacheScope}.
 * <p>
 * MCP Java SDK 2.0's {@code McpSchema.Result} interface only declares {@code meta()},
 * and {@code CallToolResult} / {@code ListToolsResult} / {@code ListResourcesResult}
 * / {@code ListPromptsResult} records do not expose the new fields (verified by
 * {@code javap -v}). When user code needs to emit a wire payload with these fields,
 * the SDK default serializer silently omits them.
 * <p>
 * This writer:
 * <ol>
 *   <li>Serializes the result via the project's shared Jackson 3 {@code JsonParser.OBJECT_MAPPER};</li>
 *   <li>Reads it back into a {@code JsonNode};</li>
 *   <li>Mutates the {@code ObjectNode} in place to add {@code resultType} (if set) and
 *       a nested {@code _cacheable} object holding {@code ttlMs} + {@code cacheScope} (if set);</li>
 *   <li>Re-serializes to a JSON string.</li>
 * </ol>
 *
 * <p>Until MCP Java SDK ≥ 2.1 exposes these fields natively, this is the
 * highest-fidelity workaround. When SDK exposure arrives, this class can be
 * removed and callers should use {@code McpSchema.CallToolResult.Builder} chained
 * with {@code .resultType(...)} / {@code .ttlMs(...)} / {@code .cacheScope(...)}.
 *
 * @author han
 * @time 2026/8/1 00:50
 */
public final class McpResultWriter {

    private McpResultWriter() {
    }

    /**
     * Write a {@link McpSchema.CallToolResult} with a {@code resultType} annotation
     * (defaults to {@link ResultTypeConvention#COMPLETE}). Returns the JSON string
     * suitable as the {@code result} field of a JSON-RPC response.
     */
    public static String writeCallToolResult(McpSchema.CallToolResult result) throws java.io.IOException {
        return write(result, ResultTypeConvention.COMPLETE, null);
    }

    /**
     * Write a {@link McpSchema.CallToolResult} with an explicit resultType and
     * optional cache hint. Pass null cache to omit the {@code _cacheable} wrapper.
     */
    public static String writeCallToolResult(McpSchema.CallToolResult result, String resultType,
                                              CacheHints.Hint cache) throws java.io.IOException {
        ResultTypeConvention.validate(resultType);
        return write(result, resultType, cache);
    }

    /**
     * Write a list-tools result with optional cache hint. The resultType is always
     * {@code "complete"} for list endpoints (MRTR is request-scoped, not list-scoped).
     */
    public static String writeListToolsResult(McpSchema.ListToolsResult result,
                                              CacheHints.Hint cache) throws java.io.IOException {
        return write(result, ResultTypeConvention.COMPLETE, cache);
    }

    public static String writeListResourcesResult(McpSchema.ListResourcesResult result,
                                                   CacheHints.Hint cache) throws java.io.IOException {
        return write(result, ResultTypeConvention.COMPLETE, cache);
    }

    public static String writeListPromptsResult(McpSchema.ListPromptsResult result,
                                                 CacheHints.Hint cache) throws java.io.IOException {
        return write(result, ResultTypeConvention.COMPLETE, cache);
    }

    /**
     * Core write path — serializes any object to JSON, then augments with
     * protocol 2026-07-28 fields that SDK 2.0 cannot express natively.
     */
    private static String write(Object result, String resultType, CacheHints.Hint cache) throws java.io.IOException {
        ObjectMapper om = JsonParser.getObjectMapper();
        JsonNode tree = om.valueToTree(result);
        if (!(tree instanceof ObjectNode obj)) {
            throw new IllegalStateException(
                "expected ObjectNode for " + result.getClass().getName() + ", got: " + tree.getClass().getName());
        }
        obj.put("resultType", resultType);
        if (cache != null) {
            // Wire convention: nest under a single key to avoid top-level pollution
            // while remaining valid JSON-RPC. The key name is configurable; users
            // may rename via the (ObjectNode, String, Hint, String) overload if their
            // client expects a different shape.
            ObjectNode cacheable = obj.putObject("_cacheable");
            cacheable.put("ttlMs", cache.ttlMs());
            cacheable.put("cacheScope", cache.cacheScope());
        }
        return om.writeValueAsString(obj);
    }

    /**
     * Escape hatch — write any result with a custom cache wrapper key (some
     * integrations expect e.g. {@code cache} instead of {@code _cacheable}).
     */
    public static String write(Object result, String resultType, CacheHints.Hint cache, String cacheWrapperKey)
            throws java.io.IOException {
        ResultTypeConvention.validate(resultType);
        ObjectMapper om = JsonParser.getObjectMapper();
        JsonNode tree = om.valueToTree(result);
        if (!(tree instanceof ObjectNode obj)) {
            throw new IllegalStateException("expected ObjectNode, got: " + tree.getClass().getName());
        }
        obj.put("resultType", resultType);
        if (cache != null) {
            ObjectNode cacheable = obj.putObject(cacheWrapperKey);
            cacheable.put("ttlMs", cache.ttlMs());
            cacheable.put("cacheScope", cache.cacheScope());
        }
        return om.writeValueAsString(obj);
    }

    public static String writeInputRequired(
            com.ai.plug.core.spec.mrtr.MrtrTypes.InputRequiredResult result) throws java.io.IOException {
        return JsonParser.getObjectMapper().writeValueAsString(result);
    }

    /**
     * Convenience: build a {@link WrappedCallToolResult} pairing the SDK
     * result with its wire JSON.
     */
    public static WrappedCallToolResult wrap(McpSchema.CallToolResult sdkResult) throws java.io.IOException {
        return new WrappedCallToolResult(sdkResult, writeCallToolResultFromMeta(sdkResult));
    }

    /**
     * Reverse-extract wire-layer hints from a {@link McpSchema.CallToolResult}'s
     * meta map (where {@link DefaultMcpCallToolResultConverter} deposits them
     * from the {@code @McpTool} annotation + MRTR/Task return paths), then
     * write the result with all the protocol 2026-07-28 fields (resultType,
     * _cacheable wrapper) properly applied.
     * <p>
     * This is the single entry point that lets a Spring AI starter or custom
     * transport read meta from a tool result and emit a wire-compliant
     * JSON-RPC payload — without each call site having to repeat the
     * meta-to-wire mapping.
     */
    public static String writeCallToolResultFromMeta(McpSchema.CallToolResult result) throws java.io.IOException {
        Map<String, Object> meta = result.meta();
        String resultType = ResultTypeConvention.COMPLETE;
        CacheHints.Hint cache = null;
        if (meta != null) {
            // resultType from converter hint; defensive fallback if invalid
            Object rt = meta.get("resultType");
            if (rt instanceof String rtStr) {
                if (ResultTypeConvention.COMPLETE.equals(rtStr) || ResultTypeConvention.INPUT_REQUIRED.equals(rtStr)) {
                    resultType = rtStr;
                }
                // else: silently fall back to complete (unknown values ignored)
            }
            // cache hint (ttlMs + cacheScope); wrapper key is config
            Object ttl = meta.get("ttlMs");
            Object scope = meta.get("cacheScope");
            if (ttl instanceof Number ttlNum && ttlNum.longValue() > 0) {
                String cacheScopeRaw = scope instanceof String s ? s : null;
                long ttlMs = ttlNum.longValue();
                // Silently fall back to private on invalid scope (avoid throwing
                // when meta carries a stale or typo'd scope from a previous schema)
                if (cacheScopeRaw == null || CacheHints.CACHE_SCOPE_PUBLIC.equals(cacheScopeRaw)
                        || CacheHints.CACHE_SCOPE_PRIVATE.equals(cacheScopeRaw)) {
                    cache = new CacheHints.Hint(ttlMs, CacheHints.normalizeScope(cacheScopeRaw));
                }
                else {
                    cache = new CacheHints.Hint(ttlMs, CacheHints.CACHE_SCOPE_PRIVATE);
                }
            }
        }
        return writeCallToolResult(result, resultType, cache);
    }

    /**
     * List-result variant: extracts ttlMs / cacheScope / cacheWrapperKey from
     * a {@code List*Result} via the same meta key set.
     */
    public static String writeListToolsResultFromMeta(McpSchema.ListToolsResult result) throws java.io.IOException {
        return writeListToolsResult(result, extractCacheHintFromSdkResult(result));
    }

    public static String writeListResourcesResultFromMeta(McpSchema.ListResourcesResult result) throws java.io.IOException {
        return writeListResourcesResult(result, extractCacheHintFromSdkResult(result));
    }

    public static String writeListPromptsResultFromMeta(McpSchema.ListPromptsResult result) throws java.io.IOException {
        return writeListPromptsResult(result, extractCacheHintFromSdkResult(result));
    }

    /**
     * List results produced by the SDK carry TTL/cache hints via meta() too —
     * currently a no-op since SDK 2.0 List*Result have no native field. Hook
     * left here for when SDK exposes them. Today this returns null.
     */
    private static CacheHints.Hint extractCacheHintFromSdkResult(Object result) {
        return null;
    }

    /**
     * Build a {@link CacheHints.Hint} from a meta map containing ttlMs /
     * cacheScope / cacheWrapperKey entries (typically the {@code @McpTool}
     * annotation via {@code DefaultMcpCallToolResultConverter}).
     * <p>
     * Used as the single entry point for converting wire-layer hints held in
     * any meta map back into a {@link CacheHints.Hint} that this writer can
     * consume.
     */
    public static CacheHints.Hint cacheHintFromMeta(@org.jspecify.annotations.Nullable Map<String, Object> meta) {
        if (meta == null) {
            return null;
        }
        Object ttl = meta.get("ttlMs");
        Object scope = meta.get("cacheScope");
        if (ttl instanceof Number ttlNum && ttlNum.longValue() > 0) {
            String cacheScope = scope instanceof String s ? s : null;
            return new CacheHints.Hint(ttlNum.longValue(),
                CacheHints.normalizeScope(cacheScope));
        }
        return null;
    }
}