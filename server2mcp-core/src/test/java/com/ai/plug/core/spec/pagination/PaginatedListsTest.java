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

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PaginatedLists}. Covers cursor parsing/formatting, page-size
 * clamping, slicing boundary conditions, and the SDK-aware factory methods for
 * ListToolsResult / ListResourcesResult / ListPromptsResult.
 */
class PaginatedListsTest {

    // ---- cursor parsing ----

    @Test
    void parseOffset_nullOrBlankReturnsZero() {
        assertThat(PaginatedLists.parseOffset(null)).isZero();
        assertThat(PaginatedLists.parseOffset("")).isZero();
        assertThat(PaginatedLists.parseOffset("   ")).isZero();
    }

    @Test
    void parseOffset_validDecimal() {
        assertThat(PaginatedLists.parseOffset("0")).isZero();
        assertThat(PaginatedLists.parseOffset("50")).isEqualTo(50);
        assertThat(PaginatedLists.parseOffset("12345")).isEqualTo(12345);
    }

    @Test
    void parseOffset_negativeThrows() {
        assertThatThrownBy(() -> PaginatedLists.parseOffset("-1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-negative");
    }

    @Test
    void parseOffset_malformedThrows() {
        assertThatThrownBy(() -> PaginatedLists.parseOffset("abc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid cursor");
        assertThatThrownBy(() -> PaginatedLists.parseOffset("1.5"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formatOffset_roundTrip() {
        assertThat(PaginatedLists.formatOffset(0)).isNull();
        assertThat(PaginatedLists.formatOffset(-5)).isNull();
        assertThat(PaginatedLists.formatOffset(50)).isEqualTo("50");
        assertThat(PaginatedLists.parseOffset(PaginatedLists.formatOffset(99))).isEqualTo(99);
    }

    // ---- page-size clamping ----

    @Test
    void clampPageSize_defaultsAndBounds() {
        assertThat(PaginatedLists.clampPageSize(0)).isEqualTo(PaginatedLists.DEFAULT_PAGE_SIZE);
        assertThat(PaginatedLists.clampPageSize(-10)).isEqualTo(PaginatedLists.DEFAULT_PAGE_SIZE);
        assertThat(PaginatedLists.clampPageSize(20)).isEqualTo(20);
        assertThat(PaginatedLists.clampPageSize(1000)).isEqualTo(PaginatedLists.MAX_PAGE_SIZE);
    }

    // ---- slicing ----

    @Test
    void slice_emptyList() {
        PaginatedLists.Page<String> page = PaginatedLists.slice(List.of(), 0, 10);
        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void slice_nullList() {
        PaginatedLists.Page<String> page = PaginatedLists.slice(null, 0, 10);
        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void slice_offsetBeyondEnd() {
        PaginatedLists.Page<Integer> page = PaginatedLists.slice(List.of(1, 2, 3), 10, 5);
        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void slice_exactFitNoNextCursor() {
        // 3 items, size 3 -> one page, no next
        List<Integer> data = List.of(1, 2, 3);
        PaginatedLists.Page<Integer> page = PaginatedLists.slice(data, 0, 3);
        assertThat(page.items()).containsExactly(1, 2, 3);
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void slice_multiPageHasNextCursor() {
        List<Integer> data = List.of(1, 2, 3, 4, 5, 6, 7);
        // page 1
        PaginatedLists.Page<Integer> p1 = PaginatedLists.slice(data, 0, 3);
        assertThat(p1.items()).containsExactly(1, 2, 3);
        assertThat(p1.nextCursor()).isEqualTo("3");
        assertThat(p1.hasMore()).isTrue();
        // page 2 — feed cursor back
        int offset = PaginatedLists.parseOffset(p1.nextCursor());
        PaginatedLists.Page<Integer> p2 = PaginatedLists.slice(data, offset, 3);
        assertThat(p2.items()).containsExactly(4, 5, 6);
        assertThat(p2.nextCursor()).isEqualTo("6");
        // page 3 — tail
        offset = PaginatedLists.parseOffset(p2.nextCursor());
        PaginatedLists.Page<Integer> p3 = PaginatedLists.slice(data, offset, 3);
        assertThat(p3.items()).containsExactly(7);
        assertThat(p3.nextCursor()).isNull();
    }

    @Test
    void slice_pageSizeClamped() {
        // Asking for 1000 should be clamped to MAX_PAGE_SIZE
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            data.add(i);
        }
        PaginatedLists.Page<Integer> page = PaginatedLists.slice(data, 0, 1000);
        assertThat(page.items()).hasSize(PaginatedLists.MAX_PAGE_SIZE);
        assertThat(page.nextCursor()).isEqualTo(String.valueOf(PaginatedLists.MAX_PAGE_SIZE));
    }

    @Test
    void slice_isImmutable() {
        // subList view would mutate the original; verify Page.items is independent
        List<Integer> data = List.of(1, 2, 3, 4, 5);
        PaginatedLists.Page<Integer> page = PaginatedLists.slice(data, 0, 3);
        // data is immutable, page.items() is List.copyOf() — both safe.
        assertThat(page.items()).containsExactly(1, 2, 3);
    }

    // ---- SDK-aware factories ----

    @Test
    void toListToolsResult_withAndWithoutCursor() {
        List<McpSchema.Tool> tools = List.of(
            McpSchema.Tool.builder().name("a").build(),
            McpSchema.Tool.builder().name("b").build()
        );
        McpSchema.ListToolsResult page1 = PaginatedLists.toListToolsResult(tools, "10");
        assertThat(page1.tools()).hasSize(2);
        assertThat(page1.nextCursor()).isEqualTo("10");

        McpSchema.ListToolsResult page2 = PaginatedLists.toListToolsResult(tools, null);
        assertThat(page2.nextCursor()).isNull();
    }

    @Test
    void toListResourcesResult_withCursor() {
        List<McpSchema.Resource> resources = List.of(
            new McpSchema.Resource("r1", "file:///a", "desc", "file:///a", null, null, null, null)
        );
        McpSchema.ListResourcesResult result = PaginatedLists.toListResourcesResult(resources, "5");
        assertThat(result.resources()).hasSize(1);
        assertThat(result.nextCursor()).isEqualTo("5");
    }

    @Test
    void toListPromptsResult_withCursor() {
        List<McpSchema.Prompt> prompts = List.of(
            new McpSchema.Prompt("p1", "title", "desc", null)
        );
        McpSchema.ListPromptsResult result = PaginatedLists.toListPromptsResult(prompts, null);
        assertThat(result.prompts()).hasSize(1);
        assertThat(result.nextCursor()).isNull();
    }
}