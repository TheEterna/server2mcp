/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.dedup;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for IdempotentCache TTL eviction:
 * - mixed expiry / non-expiry entries
 * - periodic eviction via evictExpired()
 * - cache stays size-bounded after multiple eviction cycles
 */
class IdempotentCacheEvictionE2ETest {

    @Test
    void evictExpired_removesOnlyExpiredEntries() throws Exception {
        IdempotentCache cache = new IdempotentCache(50); // 50ms TTL

        // Three entries all added within the same instant
        String freshFp = cache.fingerprint("fresh", Map.of("a", 1));
        cache.put(freshFp, "fresh");

        // Wait for the first entry to expire
        Thread.sleep(80);

        // Add another entry that's NOT yet expired
        String newFp = cache.fingerprint("new", Map.of("b", 2));
        cache.put(newFp, "new");

        // Evict — should remove the first (expired) but keep the second
        int removed = cache.evictExpired();
        assertThat(removed).isEqualTo(1);
        assertThat(cache.size()).isEqualTo(1);
        // The new entry remains
        assertThat(cache.contains(newFp)).isTrue();
        // The old one is gone
        assertThat(cache.contains(freshFp)).isFalse();
    }

    @Test
    void evictExpired_emptyCacheIsNoOp() {
        IdempotentCache cache = new IdempotentCache(60_000);
        int removed = cache.evictExpired();
        assertThat(removed).isZero();
        assertThat(cache.size()).isZero();
    }

    @Test
    void evictExpired_allFreshIsNoOp() {
        IdempotentCache cache = IdempotentCache.withTtl(5, TimeUnit.SECONDS);
        cache.put(cache.fingerprint("a", Map.of()), "x");
        cache.put(cache.fingerprint("b", Map.of()), "y");
        int removed = cache.evictExpired();
        assertThat(removed).isZero();
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    void evictExpired_allExpired() throws Exception {
        IdempotentCache cache = new IdempotentCache(30);
        for (int i = 0; i < 5; i++) {
            cache.put(cache.fingerprint("k", Map.of("i", i)), "v" + i);
        }
        Thread.sleep(60);
        int removed = cache.evictExpired();
        assertThat(removed).isEqualTo(5);
        assertThat(cache.size()).isZero();
    }

    @Test
    void evictExpired_calledMultipleTimes_boundedMemory() throws Exception {
        IdempotentCache cache = new IdempotentCache(40);
        // Round 1: add 3, evict
        for (int i = 0; i < 3; i++) {
            cache.put(cache.fingerprint("k", Map.of("i", i)), "v" + i);
        }
        Thread.sleep(60);
        cache.evictExpired();
        assertThat(cache.size()).isZero();

        // Round 2: add 2 more, evict
        for (int i = 0; i < 2; i++) {
            cache.put(cache.fingerprint("k", Map.of("i", i + 10)), "v" + i);
        }
        Thread.sleep(60);
        cache.evictExpired();
        assertThat(cache.size()).isZero();

        // Cache is empty — no memory leak from accumulated entries
    }

    @Test
    void putAfterExpire_maintainsFunctionalBehavior() throws Exception {
        IdempotentCache cache = new IdempotentCache(30);
        String fp = cache.fingerprint("tool", Map.of("x", 1));
        cache.put(fp, "v1");
        Thread.sleep(60);

        // After expire, putting the same fingerprint should re-add
        cache.put(fp, "v2");
        assertThat(cache.contains(fp)).isTrue();
        assertThat(cache.get(fp, String.class)).isEqualTo("v2");
        assertThat(cache.size()).isEqualTo(1);
    }
}