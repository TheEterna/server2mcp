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

class IdempotentCacheTest {

    @Test
    void sameArgs_sameFingerprint() {
        IdempotentCache cache = new IdempotentCache(60_000);
        String fp1 = cache.fingerprint("tool", Map.of("a", 1, "b", "x"));
        String fp2 = cache.fingerprint("tool", Map.of("a", 1, "b", "x"));
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void differentArgs_differentFingerprint() {
        IdempotentCache cache = new IdempotentCache(60_000);
        String fp1 = cache.fingerprint("tool", Map.of("a", 1));
        String fp2 = cache.fingerprint("tool", Map.of("a", 2));
        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    void differentTool_differentFingerprint() {
        IdempotentCache cache = new IdempotentCache(60_000);
        String fp1 = cache.fingerprint("tool-a", Map.of("x", 1));
        String fp2 = cache.fingerprint("tool-b", Map.of("x", 1));
        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    void putGet_roundTrips() {
        IdempotentCache cache = new IdempotentCache(60_000);
        String fp = cache.fingerprint("tool", Map.of("k", "v"));
        assertThat(cache.contains(fp)).isFalse();
        cache.put(fp, "result-payload");
        assertThat(cache.contains(fp)).isTrue();
        assertThat(cache.get(fp, String.class)).isEqualTo("result-payload");
    }

    @Test
    void putThenInvalidate_gone() {
        IdempotentCache cache = new IdempotentCache(60_000);
        String fp = cache.fingerprint("tool", Map.of("k", "v"));
        cache.put(fp, "x");
        cache.invalidate(fp);
        assertThat(cache.contains(fp)).isFalse();
    }

    @Test
    void expiredEntry_returnsNull() throws Exception {
        IdempotentCache cache = new IdempotentCache(50); // 50ms
        String fp = cache.fingerprint("tool", Map.of("k", "v"));
        cache.put(fp, "x");
        assertThat(cache.contains(fp)).isTrue();
        Thread.sleep(80);
        assertThat(cache.contains(fp)).isFalse();
        assertThat(cache.get(fp, String.class)).isNull();
    }

    @Test
    void evictExpired_removesExpired() throws Exception {
        IdempotentCache cache = new IdempotentCache(50);
        cache.put(cache.fingerprint("a", Map.of()), "x");
        cache.put(cache.fingerprint("b", Map.of()), "y");
        Thread.sleep(80);
        // Add a fresh one
        cache.put(cache.fingerprint("c", Map.of()), "z");
        int removed = cache.evictExpired();
        assertThat(removed).isEqualTo(2);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void clear_emptiesStore() {
        IdempotentCache cache = new IdempotentCache(60_000);
        cache.put(cache.fingerprint("t", Map.of()), "x");
        cache.clear();
        assertThat(cache.size()).isZero();
    }

    @Test
    void withTtl_factory() {
        IdempotentCache c = IdempotentCache.withTtl(5, TimeUnit.SECONDS);
        String fp = c.fingerprint("t", Map.of());
        c.put(fp, "x");
        assertThat(c.contains(fp)).isTrue();
    }

    @Test
    void negativeTtlRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new IdempotentCache(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}