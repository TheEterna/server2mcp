package com.ai.plug.core.spec.meta;

import java.util.HashMap;
import java.util.Map;

/**
 * Helpers for the {@code _meta} field carried on every MCP request / response /
 * notification (MCP protocol 2025-11-25). Centralizes the agreed-upon reserved
 * keys so that user code doesn't have to repeat string literals.
 *
 * <h2>OpenTelemetry trace context propagation (SEP-414)</h2>
 * Three keys are reserved by spec: {@code traceparent}, {@code tracestate},
 * {@code baggage}. Servers SHOULD forward them verbatim between the request's
 * {@code _meta} and any nested server-initiated calls, so that end-to-end
 * tracing stays consistent.
 *
 * @author han
 * @time 2026/7/31 19:08
 */
public final class MetaUtils {

    // --- OTel / W3C trace context (SEP-414) ---
    public static final String TRACE_PARENT = "traceparent";
    public static final String TRACE_STATE = "tracestate";
    public static final String BAGGAGE = "baggage";

    // --- pagination / progress / tasks extension ---
    /** progressToken is reserved by the protocol for long-running notifications. */
    public static final String PROGRESS_TOKEN = "progressToken";
    /** clients SHOULD set io.modelcontextprotocol/clientCapabilities. */
    public static final String CLIENT_CAPABILITIES = "io.modelcontextprotocol/clientCapabilities";
    /** servers SHOULD set io.modelcontextprotocol/serverCapabilities. */
    public static final String SERVER_CAPABILITIES = "io.modelcontextprotocol/serverCapabilities";
    /** clients SHOULD set io.modelcontextprotocol/protocolVersion. */
    public static final String PROTOCOL_VERSION = "io.modelcontextprotocol/protocolVersion";
    /** clients SHOULD set io.modelcontextprotocol/clientInfo. */
    public static final String CLIENT_INFO = "io.modelcontextprotocol/clientInfo";

    private MetaUtils() {
    }

    /**
     * Forward trace context from a request's _meta to an outgoing server-side
     * call. Returns a new map containing the three reserved keys if and only if
     * the source contains at least one of them — callers can pass the result
     * directly as their call's _meta without filtering.
     *
     * @return non-null map (possibly empty); never null
     */
    public static Map<String, Object> forwardTraceContext(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> forward = new HashMap<>(3);
        copyIfPresent(source, forward, TRACE_PARENT);
        copyIfPresent(source, forward, TRACE_STATE);
        copyIfPresent(source, forward, BAGGAGE);
        return forward;
    }

    /**
     * Merge two _meta maps with source taking precedence over base. Returns a
     * new map; inputs are not mutated.
     */
    public static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> source) {
        Map<String, Object> merged = new HashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (source != null) {
            merged.putAll(source);
        }
        return merged;
    }

    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        Object v = source.get(key);
        if (v != null) {
            target.put(key, v);
        }
    }
}