/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"));
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageCacheTest {

    @Test
    void wrap_trimsToPagingSize() {
        // 100 items, page size 10, offset 50 -> 10 items
        List<Integer> all = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) all.add(i);
        McpPaging paging = new McpPaging(50, 10);
        PageList<Integer> page = PageCache.wrap(all, 100, paging);
        assertThat(page.items()).hasSize(10);
        assertThat(page.items().get(0)).isEqualTo(50);
        assertThat(page.items().get(9)).isEqualTo(59);
        assertThat(page.totalItems()).isEqualTo(100);
    }

    @Test
    void wrap_lastPageSmaller() {
        // 100 items, page size 10, offset 95 -> 5 items
        List<Integer> all = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) all.add(i);
        McpPaging paging = new McpPaging(95, 10);
        PageList<Integer> page = PageCache.wrap(all, 100, paging);
        assertThat(page.items()).hasSize(5);
        assertThat(page.items().get(0)).isEqualTo(95);
    }

    @Test
    void wrap_offsetBeyondEndReturnsEmpty() {
        List<Integer> all = List.of(1, 2, 3);
        McpPaging paging = new McpPaging(100, 10);
        PageList<Integer> page = PageCache.wrap(all, 3, paging);
        assertThat(page.items()).isEmpty();
    }

    @Test
    void wrap_withRequestMeta_extractsCursorAndSize() {
        List<Integer> all = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) all.add(i);
        Map<String, Object> meta = Map.of("cursor", "10", "pageSize", 5);
        PageList<Integer> page = PageCache.wrap(all, 50, meta);
        assertThat(page.items()).hasSize(5);
        assertThat(page.items().get(0)).isEqualTo(10);
    }

    @Test
    void wrap_withNullMeta_usesDefaults() {
        List<Integer> all = List.of(1, 2, 3);
        // null can match either overload; we want the McpPaging variant
        // (defaults to offset=0, size=DEFAULT_PAGE_SIZE).
        PageList<Integer> page = com.ai.plug.core.spec.pagination.PageCache.wrap(
            all, 3, (com.ai.plug.core.spec.pagination.McpPaging) null);
        assertThat(page.items()).hasSize(3); // all 3 fit
    }
}