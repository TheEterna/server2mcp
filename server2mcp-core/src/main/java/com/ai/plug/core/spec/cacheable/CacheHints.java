package com.ai.plug.core.spec.cacheable;

import java.time.Duration;

/**
 * Convention for MCP cacheable results (protocol 2026-07-28 SEP-2549, {@code CacheableResult}).
 * <p>
 * MCP 2026-07-28 requires {@code tools/list}, {@code prompts/list},
 * {@code resources/list}, {@code resources/read}, and {@code resources/templates/list}
 * results to carry {@code ttlMs} and {@code cacheScope} fields:
 * <ul>
 *   <li>{@code ttlMs} — freshness hint in milliseconds</li>
 *   <li>{@code cacheScope} — {@code "public"} or {@code "private"}; controls whether
 *       shared intermediaries may cache the response</li>
 * </ul>
 *
 * <h2>当前 SDK 状态（2.0）</h2>
 * MCP Java SDK 2.0's {@code ListToolsResult} / {@code ListResourcesResult} /
 * {@code ListPromptsResult} records do <b>not</b> expose {@code ttlMs} or
 * {@code cacheScope} fields (verified by {@code javap -v}). The
 * {@code McpSchema.CacheableResult} interface does not exist in SDK 2.0.
 *
 * <h2>本框架的角色</h2>
 * This class centralizes the protocol-level constants and a small adapter API so
 * user code can author cache hints today, and a single SDK migration shim can
 * route them through SDK builders when SDK 2.1+ lands.
 *
 * <p>设计取舍：缓存语义属于路由策略决定（调用方应决定 cacheScope 与 ttlMs），
 * 本框架不替用户决定，仅提供类型安全的常量与轻量助手。
 *
 * @author han
 * @time 2026/8/1 00:18
 */
public final class CacheHints {

    public static final String CACHE_SCOPE_PUBLIC = "public";
    public static final String CACHE_SCOPE_PRIVATE = "private";

    /** Default ttlMs — 60s — chosen as a conservative default for protocol 2025-11-25 use cases. */
    public static final long DEFAULT_TTL_MS = 60_000L;

    /** ttlMs=0 — explicit "do not cache" (per spec semantics). */
    public static final long NO_CACHE_TTL_MS = 0L;

    private CacheHints() {
    }

    /**
     * Convert a {@link Duration} to ttlMs. Null maps to {@link #DEFAULT_TTL_MS}.
     */
    public static long toTtlMs(Duration ttl) {
        if (ttl == null) {
            return DEFAULT_TTL_MS;
        }
        return Math.max(0L, ttl.toMillis());
    }

    /**
     * Validate a cacheScope string. Null / blank maps to {@link #CACHE_SCOPE_PRIVATE}
     * (the conservative default — a tool/resource listing by default should not be
     * cacheable by intermediaries that may serve different tenants).
     */
    public static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return CACHE_SCOPE_PRIVATE;
        }
        if (!CACHE_SCOPE_PUBLIC.equals(scope) && !CACHE_SCOPE_PRIVATE.equals(scope)) {
            throw new IllegalArgumentException(
                "cacheScope must be '" + CACHE_SCOPE_PUBLIC + "' or '" + CACHE_SCOPE_PRIVATE
                    + "', got: " + scope);
        }
        return scope;
    }

    /**
     * Build a {@link CacheHints.Hint} record for direct attachment to a result.
     * Centralizes validation so user code cannot accidentally emit invalid
     * protocol values.
     */
    public static Hint of(Duration ttl, String scope) {
        return new Hint(toTtlMs(ttl), normalizeScope(scope));
    }

    /** Immutable cache hint value object. */
    public record Hint(long ttlMs, String cacheScope) {

        public Hint {
            if (ttlMs < 0) {
                throw new IllegalArgumentException("ttlMs must be >= 0, got: " + ttlMs);
            }
            if (!CACHE_SCOPE_PUBLIC.equals(cacheScope) && !CACHE_SCOPE_PRIVATE.equals(cacheScope)) {
                throw new IllegalArgumentException("cacheScope invalid: " + cacheScope);
            }
        }
    }
}