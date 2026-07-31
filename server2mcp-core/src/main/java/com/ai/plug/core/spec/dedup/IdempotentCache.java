package com.ai.plug.core.spec.dedup;

import com.ai.plug.common.utils.JsonParser;
import tools.jackson.core.JacksonException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tiny TTL-bounded cache for idempotent tool call deduplication.
 * <p>
 * When {@link com.ai.plug.core.annotation.McpTool#idempotentHint()} is true,
 * the framework fingerprints each invocation's (tool name + arguments) tuple
 * and reuses the prior {@code CallToolResult} if the same fingerprint arrives
 * within the configured TTL window — saving redundant downstream calls
 * (database reads, idempotent API calls, etc.).
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var cache = new IdempotentCache(60_000); // 60s TTL
 *   String fp = cache.fingerprint(toolName, args);
 *   if (cache.contains(fp)) {
 *       return cache.get(fp);
 *   }
 *   CallToolResult result = doWork();
 *   cache.put(fp, result);
 *   return result;
 * }</pre>
 *
 * <h2>限制</h2>
 * Single-process, in-memory only. For cluster-wide dedup, swap this out
 * with a Redis-backed cache — the interface is small enough.
 *
 * @author han
 * @time 2026/8/1 05:49
 */
public class IdempotentCache {

    private final long ttlMs;
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public IdempotentCache(long ttlMs) {
        if (ttlMs < 0) {
            throw new IllegalArgumentException("ttlMs must be >= 0, got: " + ttlMs);
        }
        this.ttlMs = ttlMs;
    }

    /**
     * Compute a stable SHA-256 fingerprint for (toolName, arguments).
     * Uses Jackson 3 for arguments serialization so Map ordering is stable.
     */
    public String fingerprint(String toolName, Map<String, Object> arguments) {
        try {
            String argsJson = JsonParser.getObjectMapper().writeValueAsString(arguments);
            String combined = toolName + "|" + argsJson;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (JacksonException | NoSuchAlgorithmException ex) {
            // Fall back to identity hashCode on combined string (still stable
            // for the same inputs in the same process)
            return Integer.toHexString((toolName + "|" + arguments).hashCode());
        }
    }

    public boolean contains(String fingerprint) {
        Entry e = store.get(fingerprint);
        return e != null && !isExpired(e);
    }

    public <T> T get(String fingerprint, Class<T> type) {
        Entry e = store.get(fingerprint);
        if (e == null || isExpired(e)) {
            return null;
        }
        return type.cast(e.value);
    }

    public void put(String fingerprint, Object value) {
        store.put(fingerprint, new Entry(value, System.currentTimeMillis() + ttlMs));
    }

    public void invalidate(String fingerprint) {
        store.remove(fingerprint);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }

    /** Periodic housekeeping — call from a scheduler to bound memory. */
    public int evictExpired() {
        long now = System.currentTimeMillis();
        int[] removed = {0};
        store.entrySet().removeIf(e -> {
            if (e.getValue().expiresAt <= now) {
                removed[0]++;
                return true;
            }
            return false;
        });
        return removed[0];
    }

    private boolean isExpired(Entry e) {
        return e.expiresAt <= System.currentTimeMillis();
    }

    private record Entry(Object value, long expiresAt) {
    }

    /** Convenience for creating a TTL from a {@link TimeUnit} duration. */
    public static IdempotentCache withTtl(long duration, TimeUnit unit) {
        return new IdempotentCache(unit.toMillis(duration));
    }
}