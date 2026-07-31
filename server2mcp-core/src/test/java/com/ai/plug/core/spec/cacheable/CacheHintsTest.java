/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.cacheable;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheHintsTest {

    @Test
    void constantsHaveExpectedValues() {
        assertThat(CacheHints.CACHE_SCOPE_PUBLIC).isEqualTo("public");
        assertThat(CacheHints.CACHE_SCOPE_PRIVATE).isEqualTo("private");
        assertThat(CacheHints.DEFAULT_TTL_MS).isEqualTo(60_000L);
    }

    @Test
    void toTtlMs_convertsDuration() {
        assertThat(CacheHints.toTtlMs(Duration.ofSeconds(30))).isEqualTo(30_000L);
        assertThat(CacheHints.toTtlMs(Duration.ofMinutes(5))).isEqualTo(300_000L);
        assertThat(CacheHints.toTtlMs(Duration.ofMillis(500))).isEqualTo(500L);
    }

    @Test
    void toTtlMs_nullDefaultsTo60s() {
        assertThat(CacheHints.toTtlMs(null)).isEqualTo(CacheHints.DEFAULT_TTL_MS);
    }

    @Test
    void toTtlMs_negativeClampsToZero() {
        // Negative durations clamp to 0, not throw — keeps the wire contract
        // (ttlMs >= 0) without surprising user code at edge cases.
        assertThat(CacheHints.toTtlMs(Duration.ofMillis(-100))).isZero();
    }

    @Test
    void normalizeScope_defaultsToPrivate() {
        assertThat(CacheHints.normalizeScope(null)).isEqualTo(CacheHints.CACHE_SCOPE_PRIVATE);
        assertThat(CacheHints.normalizeScope("")).isEqualTo(CacheHints.CACHE_SCOPE_PRIVATE);
        assertThat(CacheHints.normalizeScope("   ")).isEqualTo(CacheHints.CACHE_SCOPE_PRIVATE);
    }

    @Test
    void normalizeScope_passesValidValues() {
        assertThat(CacheHints.normalizeScope("public")).isEqualTo("public");
        assertThat(CacheHints.normalizeScope("private")).isEqualTo("private");
    }

    @Test
    void normalizeScope_rejectsUnknown() {
        assertThatThrownBy(() -> CacheHints.normalizeScope("shared"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("shared");
    }

    @Test
    void of_createsValidHint() {
        CacheHints.Hint hint = CacheHints.of(Duration.ofSeconds(30), "public");
        assertThat(hint.ttlMs()).isEqualTo(30_000L);
        assertThat(hint.cacheScope()).isEqualTo("public");
    }

    @Test
    void of_withNulls_usesDefaults() {
        CacheHints.Hint hint = CacheHints.of(null, null);
        assertThat(hint.ttlMs()).isEqualTo(CacheHints.DEFAULT_TTL_MS);
        assertThat(hint.cacheScope()).isEqualTo(CacheHints.CACHE_SCOPE_PRIVATE);
    }

    @Test
    void of_rejectsInvalidScope() {
        assertThatThrownBy(() -> CacheHints.of(Duration.ofSeconds(30), "garbage"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hintRecord_validatesOnConstruction() {
        // ttlMs < 0 — guarded by record canonical constructor
        assertThatThrownBy(() -> new CacheHints.Hint(-1L, "public"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CacheHints.Hint(60_000L, "foo"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}