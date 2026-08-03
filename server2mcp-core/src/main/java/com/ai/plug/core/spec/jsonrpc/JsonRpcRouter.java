package com.ai.plug.core.spec.jsonrpc;

import com.ai.plug.core.spec.meta.MetaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Routes a {@link JsonRpcRequest} to a registered handler by method name.
 *
 * <p>Handlers are plain {@link Function}s from {@code params → result}; this
 * keeps the surface minimal and lets us register protocol-2026-07-28-only
 * routes (the ones SDK 2.0 doesn't ship) without inventing new
 * abstractions.
 *
 * <h2>注册示例</h2>
 * <pre>{@code
 *   router.register("server/discover", params -> discoverEndpoint.negotiate(...));
 *   router.register("tasks/cancel", params -> {
 *       String taskId = (String) params.get("taskId");
 *       return tasksEndpoint.handleCancel(taskId, null);
 *   });
 * }</pre>
 *
 * <h2>异常策略</h2>
 *
 * <p>Handler exceptions are caught and translated to a JSON-RPC
 * {@code INTERNAL_ERROR} response (code -32603) carrying the exception's
 * simple class name in {@code message}. We deliberately do NOT propagate
 * stack traces over the wire — callers that need more detail can enable
 * debug logging on this class.
 *
 * @author han
 * @time 2026/8/3
 */
public final class JsonRpcRouter {

    private static final Logger logger = LoggerFactory.getLogger(JsonRpcRouter.class);

    private final Map<String, Function<Map<String, Object>, Object>> handlers =
        new ConcurrentHashMap<>();

    /** Register {@code handler} for {@code method}. Replaces any previous
     *  registration under the same name (last-write-wins). */
    public void register(String method, Function<Map<String, Object>, Object> handler) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method is required");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler is required");
        }
        handlers.put(method, handler);
        logger.debug("Registered JSON-RPC handler for method={}", method);
    }

    /** Unregister {@code method}. No-op if absent. */
    public void unregister(String method) {
        handlers.remove(method);
    }

    /** @return number of registered handlers (operational metric). */
    public int registeredCount() {
        return handlers.size();
    }

    /** Dispatch {@code request} to the matching handler (or return a
     *  {@code METHOD_NOT_FOUND} error if no handler is registered). */
    public JsonRpcResponse dispatch(JsonRpcRequest request) {
        Function<Map<String, Object>, Object> handler = handlers.get(request.method());
        if (handler == null) {
            logger.debug("No handler for method={}", request.method());
            return new JsonRpcResponse(
                "2.0", null,
                JsonRpcResponse.JsonRpcError.of(
                    JsonRpcResponse.JsonRpcError.METHOD_NOT_FOUND,
                    "Method not found: " + request.method()),
                request.id(), mintMeta(request.params()));
        }
        Map<String, Object> params = request.params() != null
            ? request.params()
            : Map.of();
        try {
            Object result = handler.apply(params);
            return JsonRpcResponse.successWithMeta(result, request.id(), mintMeta(params));
        } catch (RuntimeException ex) {
            logger.error("Handler for method={} failed: {}", request.method(), ex.toString());
            return new JsonRpcResponse(
                "2.0", null,
                JsonRpcResponse.JsonRpcError.of(
                    JsonRpcResponse.JsonRpcError.INTERNAL_ERROR,
                    ex.getClass().getSimpleName() + ": " + ex.getMessage()),
                request.id(), mintMeta(params));
        }
    }

    /** Mint a fresh W3C traceparent for every response (SEP-414). If
     *  the caller supplied one in {@code params._meta}, propagate it
     *  verbatim instead. */
    private Map<String, Object> mintMeta(Map<String, Object> params) {
        Map<String, Object> incoming = null;
        Object metaObj = params == null ? null : params.get("_meta");
        if (metaObj instanceof Map<?, ?> raw) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) raw;
            incoming = m;
        }
        Map<String, Object> meta = new HashMap<>();
        if (incoming != null) {
            meta.putAll(incoming);
        }
        MetaUtils.ensureTraceparent(meta);
        return meta;
    }

    /** Convenience: dispatch from raw maps (e.g. controller deserialized JSON). */
    public JsonRpcResponse dispatchRaw(Object rawRequest) {
        if (!(rawRequest instanceof Map<?, ?> raw)) return null;
        try {
            JsonRpcRequest request = parseRaw(raw);
            return dispatch(request);
        } catch (IllegalArgumentException ex) {
            return new JsonRpcResponse(
                "2.0", null,
                JsonRpcResponse.JsonRpcError.of(
                    JsonRpcResponse.JsonRpcError.INVALID_REQUEST,
                    ex.getMessage()),
                null, mintMeta(null));
        }
    }

    private static JsonRpcRequest parseRaw(Map<?, ?> raw) {
        Object jsonrpc = raw.get("jsonrpc");
        Object method = raw.get("method");
        Object params = raw.get("params");
        Object id = raw.get("id");
        if (!(params == null || params instanceof Map)) {
            throw new IllegalArgumentException("params must be an object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> paramsMap = (Map<String, Object>) params;
        return new JsonRpcRequest(
            String.valueOf(jsonrpc),
            String.valueOf(method),
            paramsMap,
            id);
    }
}