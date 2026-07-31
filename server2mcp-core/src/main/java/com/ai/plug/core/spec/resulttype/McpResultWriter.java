package com.ai.plug.core.spec.resulttype;

import com.ai.plug.common.utils.JsonParser;
import com.ai.plug.core.spec.cacheable.CacheHints;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

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

    /**
     * Write an MRTR {@code InputRequiredResult} interim response. The
     * {@code resultType} is forced to {@code "input_required"} regardless of
     * caller-supplied value (defense in depth — record's compact constructor
     * also validates this).
     *
     * <p>Caller should pass the resulting JSON string as the {@code result}
     * field of a JSON-RPC response whose {@code id} matches the original
     * tool call. The client will then retry the request with
     * {@code inputResponses}.
     */
    public static String writeInputRequired(
            com.ai.plug.core.spec.mrtr.MrtrTypes.InputRequiredResult result) throws java.io.IOException {
        return JsonParser.getObjectMapper().writeValueAsString(result);
    }
}