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

import com.ai.plug.core.annotation.McpTool;
import com.ai.plug.core.spec.callback.tool.AbstractMcpToolMethodCallback;
import com.ai.plug.core.spec.callback.tool.DefaultMcpCallToolResultConverter;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageListTest {

    @Test
    void of_basicValues() {
        PageList<String> p = PageList.of(List.of("a", "b"), 100);
        assertThat(p.items()).containsExactly("a", "b");
        assertThat(p.totalItems()).isEqualTo(100);
    }

    @Test
    void empty_hasNoItems() {
        PageList<String> p = PageList.empty();
        assertThat(p.items()).isEmpty();
        assertThat(p.totalItems()).isZero();
    }

    @Test
    void emptyString() {
        PageList<String> p = PageList.of(List.of(), 0);
        assertThat(p.items()).isEmpty();
    }

    @Test
    void negativeTotalRejected() {
        assertThatThrownBy(() -> PageList.of(List.of(), -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullItemsRejected() {
        assertThatThrownBy(() -> PageList.of(null, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nextCursor_offsetsAt0() {
        PageList<String> p = PageList.of(List.of("a"), 100);
        // offset 0, size 50: nextOffset=50 < 100 -> "50"
        assertThat(p.nextCursor(new McpPaging(0, 50))).isEqualTo("50");
    }

    @Test
    void nextCursor_lastPageReturnsNull() {
        PageList<String> p = PageList.of(List.of("a"), 100);
        // offset 80, size 50: nextOffset=130 >= 100 -> null
        assertThat(p.nextCursor(new McpPaging(80, 50))).isNull();
    }

    // ---- converter integration ----

    @Test
    void converterPageListBranch_injectsNextCursorAndTotalItems() throws Exception {
        Method m = PageListHolder.class.getDeclaredMethod("paged", McpPaging.class);
        PageListCallback cb = new PageListCallback(m, m.getAnnotation(McpTool.class));
        // Simulate a tool returning PageList<String>(items=[a,b], total=100)
        PageList<String> pageList = PageList.of(List.of("a", "b"), 100);
        DefaultMcpCallToolResultConverter converter = new DefaultMcpCallToolResultConverter();
        // Manual capture: inject paging via the callback's capturePaging
        cb.capturePaging(new McpPaging(0, 50));
        McpSchema.CallToolResult result = converter.convertToCallToolResult(pageList, PageList.class, cb);

        // Items serialized as text content
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertThat(text).contains("a").contains("b");
        // Meta carries nextCursor + totalItems
        Map<String, Object> meta = result.meta();
        assertThat(meta).containsEntry("nextCursor", "50");
        assertThat(meta).containsEntry("totalItems", 100);
    }

    @Test
    void converterPageListBranch_lastPageNoNextCursor() throws Exception {
        Method m = PageListHolder.class.getDeclaredMethod("paged", McpPaging.class);
        PageListCallback cb = new PageListCallback(m, m.getAnnotation(McpTool.class));
        cb.capturePaging(new McpPaging(80, 50));
        PageList<String> pageList = PageList.of(List.of("a"), 100);
        DefaultMcpCallToolResultConverter converter = new DefaultMcpCallToolResultConverter();
        McpSchema.CallToolResult result = converter.convertToCallToolResult(pageList, PageList.class, cb);

        Map<String, Object> meta = result.meta();
        assertThat(meta).doesNotContainKey("nextCursor");
        assertThat(meta).containsEntry("totalItems", 100);
    }

    // ---- helper ----

    private static final class PageListCallback extends AbstractMcpToolMethodCallback {
        PageListCallback(Method method, McpTool annotation) {
            super(method, new Object(), "name", null, "{}", null, null, null, annotation, null,
                null, null);
        }
        @Override
        protected boolean isExchangeType(Class<?> paramType) { return false; }
    }

    static final class PageListHolder {
        @McpTool(name = "x")
        public PageList<String> paged(McpPaging paging) { return PageList.empty(); }
    }
}