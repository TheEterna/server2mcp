package com.ai.plug.core.spec.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * JSON-RPC 2.0 request envelope — the wire shape every MCP method MUST
 * accept. Authoritative spec: <a
 * href="https://www.jsonrpc.org/specification">jsonrpc.org/specification</a>.
 *
 * <p>Per the JSON-RPC 2.0 spec this class is a wire envelope, not a Java
 * method signature: the {@code method} string is a free-form identifier
 * (e.g. {@code "tools/list"}, {@code "tasks/cancel"}) that the
 * {@link JsonRpcRouter} dispatches on, and {@code params} is a JSON object
 * whose shape is dictated by the individual MCP method.
 *
 * <h2>为什么绕过 SDK</h2>
 *
 * <p>Java MCP SDK 2.0 has no first-class JSON-RPC dispatch: methods like
 * {@code tools/list} and {@code prompts/get} are wired through
 * {@code McpSyncServer} handlers, but protocol-2026-07-28-only routes
 * ({@code server/discover}, {@code tasks/*}, {@code subscriptions/listen})
 * have no SDK-equivalent. The board authorised us to ship our own JSON-RPC
 * envelope (SDK upgrade will replace this with the SDK's native route
 * table, but the wire shape on the network stays identical).
 *
 * @author han
 * @time 2026/8/3
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcRequest(
    @JsonProperty("jsonrpc") String jsonrpc,
    @JsonProperty("method") String method,
    @JsonProperty("params") Map<String, Object> params,
    @JsonProperty("id") Object id
) {

    public JsonRpcRequest {
        if (!"2.0".equals(jsonrpc)) {
            throw new IllegalArgumentException(
                "jsonrpc must be \"2.0\", got: " + jsonrpc);
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method is required");
        }
    }

    /** Convenience factory for a positional-id call (most MCP clients). */
    public static JsonRpcRequest of(String method, Map<String, Object> params, Object id) {
        return new JsonRpcRequest("2.0", method, params, id);
    }
}