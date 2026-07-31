/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.pagination;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link McpPaging}.
 */
class McpPagingTest {

    @Test
    void defaults_returnsOffset0DefaultSize() {
        McpPaging p = McpPaging.defaults();
        assertThat(p.offset()).isZero();
        assertThat(p.size()).isEqualTo(McpPaging.DEFAULT_PAGE_SIZE);
    }

    @Test
    void of_nullsDefault() {
        McpPaging p = McpPaging.of(null, null);
        assertThat(p.offset()).isZero();
        assertThat(p.size()).isEqualTo(McpPaging.DEFAULT_PAGE_SIZE);
    }

    @Test
    void of_validValues() {
        McpPaging p = McpPaging.of(100, 25);
        assertThat(p.offset()).isEqualTo(100);
        assertThat(p.size()).isEqualTo(25);
    }

    @Test
    void of_negativeOffsetClampsToZero() {
        McpPaging p = McpPaging.of(-50, 10);
        assertThat(p.offset()).isZero();
    }

    @Test
    void of_sizeClampedToMax() {
        McpPaging p = McpPaging.of(0, 10_000);
        assertThat(p.size()).isEqualTo(McpPaging.MAX_PAGE_SIZE);
    }

    @Test
    void of_nonPositiveSizeDefaults() {
        McpPaging p = McpPaging.of(0, -10);
        assertThat(p.size()).isEqualTo(McpPaging.DEFAULT_PAGE_SIZE);
    }

    @Test
    void fromCursor_blankDefaults() {
        McpPaging p = McpPaging.fromCursor(null, 20);
        assertThat(p.offset()).isZero();
        assertThat(p.size()).isEqualTo(20);
    }

    @Test
    void fromCursor_valid() {
        McpPaging p = McpPaging.fromCursor("50", null);
        assertThat(p.offset()).isEqualTo(50);
        assertThat(p.size()).isEqualTo(McpPaging.DEFAULT_PAGE_SIZE);
    }

    @Test
    void fromCursor_invalidDefaults() {
        assertThatThrownBy(() -> McpPaging.fromCursor("not-a-number", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nextCursor_returnsOffsetPlusSizeWhenMoreItems() {
        McpPaging p = McpPaging.of(0, 50);
        assertThat(p.nextCursor(200)).isEqualTo("50");
    }

    @Test
    void nextCursor_returnsNullWhenAtEnd() {
        McpPaging p = McpPaging.of(150, 50);
        // total=200, offset=150, size=50 -> nextOffset=200 >= 200 -> null
        assertThat(p.nextCursor(200)).isNull();
    }

    @Test
    void nextCursor_returnsNullWhenOvershoot() {
        McpPaging p = McpPaging.of(150, 50);
        // total=180, nextOffset=200 >= 180 -> null
        assertThat(p.nextCursor(180)).isNull();
    }
}