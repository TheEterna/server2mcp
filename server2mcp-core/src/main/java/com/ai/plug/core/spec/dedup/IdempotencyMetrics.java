package com.ai.plug.core.spec.dedup;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Atomic counters for tracking {@link IdempotentCache} hit/miss rates.
 * Exposes simple getters that can be wired to Micrometer / Prometheus
 * / StatsD in the user's scheduler or actuator setup.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   IdempotencyMetrics metrics = new IdempotencyMetrics();
 *   IdempotentCache cache = new IdempotencyCache(60_000, metrics);
 *
 *   // In a Micrometer configuration:
 *   MeterRegistry registry = ...;
 *   Gauge.builder("idempotent_cache_hits", metrics, m -> m.hits())
 *       .register(registry);
 *   Gauge.builder("idempotent_cache_misses", metrics, m -> m.misses())
 *       .register(registry);
 *   Gauge.builder("idempotent_cache_evictions", metrics, m -> m.evictions())
 *       .register(registry);
 * }</pre>
 *
 * <p>Counters are atomic; concurrent {@link IdempotentCache} operations on
 * multiple threads are safe.
 */
public class IdempotencyMetrics {

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    /** Called by {@link IdempotentCache#contains(String)} when the fingerprint was present. */
    public void incrementHits() {
        hits.incrementAndGet();
    }

    /** Called by {@link IdempotentCache#contains(String)} when the fingerprint was absent. */
    public void incrementMisses() {
        misses.incrementAndGet();
    }

    /** Called by {@link IdempotentCache#evictExpired()} for each removed entry. */
    public void incrementEvictions(long n) {
        evictions.addAndGet(n);
    }

    public long hits() {
        return hits.get();
    }

    public long misses() {
        return misses.get();
    }

    public long evictions() {
        return evictions.get();
    }

    /** Total lookups (hits + misses). */
    public long totalLookups() {
        return hits.get() + misses.get();
    }

    /** @return 0..1; 0 means no lookups yet, 1 means every lookup hit. */
    public double hitRate() {
        long total = totalLookups();
        return total == 0 ? 0.0 : (double) hits.get() / total;
    }

    /** Reset all counters to zero — useful for tests and ad-hoc audits. */
    public void reset() {
        hits.set(0);
        misses.set(0);
        evictions.set(0);
    }
}