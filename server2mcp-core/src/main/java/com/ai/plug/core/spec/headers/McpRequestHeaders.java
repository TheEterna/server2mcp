package com.ai.plug.core.spec.headers;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standard MCP HTTP request headers (MCP protocol 2026-07-28 SEP-2243) and a
 * helper for emitting them on every Streamable HTTP POST request.
 * <p>
 * The protocol mandates:
 * <ul>
 *   <li>{@code Mcp-Method} — the JSON-RPC method name (e.g. {@code "tools/call"});</li>
 *   <li>{@code Mcp-Name} — the tool/resource/prompt name when the method targets one;</li>
 *   <li>{@code x-mcp-header} — passthrough header carrying tool-parameter-derived custom headers
 *       (used so transport-layer reverse proxies can route / meter without parsing the JSON body).</li>
 * </ul>
 *
 * <h2>当前 SDK 状态（2.0）</h2>
 * MCP Java SDK 2.0's {@code HttpHeaders} interface declares only the legacy
 * constants ({@code MCP_SESSION_ID}, {@code LAST_EVENT_ID},
 * {@code PROTOCOL_VERSION}, {@code CONTENT_LENGTH}, {@code CONTENT_TYPE},
 * {@code ACCEPT}, {@code CACHE_CONTROL}) — the new {@code Mcp-Method} /
 * {@code Mcp-Name} / {@code x-mcp-header} are absent (verified by javap).
 *
 * <h2>本框架的角色</h2>
 * Provide the constants and a small builder so user code can attach the
 * required headers to any custom Spring / WebClient / OkHttp request before
 * it hits the MCP server's transport layer. This is a thin, zero-side-effect
 * utility — actual HTTP wire transport remains the caller's responsibility.
 *
 * @author han
 * @time 2026/8/1 02:02
 */
public final class McpRequestHeaders {

    // ---- protocol 2026-07-28 standard headers (SEP-2243) ----
    public static final String MCP_METHOD = "Mcp-Method";
    public static final String MCP_NAME = "Mcp-Name";
    public static final String X_MCP_HEADER = "x-mcp-header";

    // ---- legacy SDK 2.0 constants exposed for forward compatibility ----
    public static final String MCP_SESSION_ID = "Mcp-Session-Id";
    public static final String MCP_PROTOCOL_VERSION = "Mcp-Protocol-Version";

    private McpRequestHeaders() {
    }

    /**
     * Build the standard set of required headers for a JSON-RPC POST against a
     * Streamable HTTP MCP transport. Callers may merge this map into their
     * existing {@code HttpHeaders} builder.
     *
     * @param method JSON-RPC method name (e.g. {@code "tools/call"}, {@code "tools/list"});
     *               passed as the {@code Mcp-Method} header. Required.
     * @param name   tool / resource / prompt name when applicable; passed as
     *               {@code Mcp-Name}. May be null/blank for methods without a
     *               single named target (e.g. {@code tools/list}).
     * @return non-null LinkedHashMap preserving insertion order (insertion
     *         order chosen for readability of wire captures)
     */
    public static Map<String, String> forJsonRpcCall(String method, String name) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method is required");
        }
        Map<String, String> headers = new LinkedHashMap<>(3);
        headers.put(MCP_METHOD, method);
        if (name != null && !name.isBlank()) {
            headers.put(MCP_NAME, name);
        }
        return headers;
    }

    /**
     * Build a single {@code x-mcp-header} value from a (key, value) pair.
     * The protocol leaves the encoding scheme up to integrators; this
     * implementation uses {@code key=value} as a sensible default and lets
     * callers override with their own encoding if needed.
     */
    public static String encodeXMcPHeader(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("x-mcp-header key is required");
        }
        if (value == null) {
            throw new IllegalArgumentException("x-mcp-header value is required");
        }
        return key + "=" + value;
    }
}