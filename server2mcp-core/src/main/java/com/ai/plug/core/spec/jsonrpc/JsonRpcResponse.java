package com.ai.plug.core.spec.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * JSON-RPC 2.0 response envelope. Exactly one of {@code result} or
 * {@code error} is populated per response; the wire format mandates
 * {@code id} echo (with {@code null} for parse-error notifications).
 *
 * @author han
 * @time 2026/8/3
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcResponse(
    @JsonProperty("jsonrpc") String jsonrpc,
    @JsonProperty("result") Object result,
    @JsonProperty("error") JsonRpcError error,
    @JsonProperty("id") Object id
) {

    public static JsonRpcResponse success(Object result, Object id) {
        return new JsonRpcResponse("2.0", result, null, id);
    }

    public static JsonRpcResponse error(JsonRpcError error, Object id) {
        return new JsonRpcResponse("2.0", null, error, id);
    }

    /**
     * JSON-RPC 2.0 error object. {@code code} follows the standard
     * pre-defined range ($-32768 = reserved, $-32000..-32099 = server
     * implementation-defined). {@code message} is human-readable;
     * {@code data} is optional supplemental payload.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record JsonRpcError(
        @JsonProperty("code") int code,
        @JsonProperty("message") String message,
        @JsonProperty("data") Map<String, Object> data
    ) {
        // Standard JSON-RPC error codes
        public static final int PARSE_ERROR = -32700;
        public static final int INVALID_REQUEST = -32600;
        public static final int METHOD_NOT_FOUND = -32601;
        public static final int INVALID_PARAMS = -32602;
        public static final int INTERNAL_ERROR = -32603;
        // Server-defined range ($-32000..-32099)
        public static final int SERVER_DEFINED_BASE = -32000;

        public static JsonRpcError of(int code, String message) {
            return new JsonRpcError(code, message, null);
        }
    }
}