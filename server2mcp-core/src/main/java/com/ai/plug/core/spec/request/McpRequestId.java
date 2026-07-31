package com.ai.plug.core.spec.request;

import org.jspecify.annotations.Nullable;

/**
 * Request identity injected into tool methods as a parameter of type
 * {@code McpRequestId} — gives a tool method the ability to identify the
 * calling request (for logging, response correlation, or downstream API
 * call-chaining).
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;Tool
 *   public List&lt;Row&gt; listUsers(McpRequestId requestId, McpPaging paging) {
 *       log.info("handling request {}", requestId.id());
 *       return dataSource.fetchSlice(paging.offset(), paging.size());
 *   }
 * }</pre>
 *
 * <h2>id 来源</h2>
 * The id is extracted from the request's {@code _meta.requestId} field per
 * protocol 2025-11-25 _meta conventions. When the caller omits it, this
 * record carries {@code null} (callers should treat null as "client didn't
 * provide an id" and may use {@link #synthetic(String)} to mint one).
 *
 * @author han
 * @time 2026/8/1 05:49
 */
public record McpRequestId(@Nullable String id) {

    /** A {@code null} id means the caller didn't provide one. */
    public static final McpRequestId NONE = new McpRequestId(null);

    /**
     * Wrap a caller-provided id (which may be null).
     */
    public static McpRequestId of(@Nullable String id) {
        return id == null ? NONE : new McpRequestId(id);
    }

    /**
     * Mint a synthetic id when the caller didn't provide one. Useful for
     * logging / correlation.
     */
    public static McpRequestId synthetic(@Nullable String prefix) {
        String p = prefix == null ? "req" : prefix;
        return new McpRequestId(p + "-" + java.util.UUID.randomUUID());
    }

    /** @return true when the caller supplied an id. */
    public boolean isPresent() {
        return id != null;
    }
}