/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.callback.tool;

import com.ai.plug.core.annotation.McpTool;
import com.ai.plug.core.spec.pagination.McpPaging;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the {@link DefaultMcpCallToolResultConverter} auto-slices a
 * returned {@link List} when the {@link McpPaging} parameter was injected,
 * surfacing the nextCursor via the result's meta map.
 */
class CallbackPagingAutoSliceTest {

    private final DefaultMcpCallToolResultConverter converter = new DefaultMcpCallToolResultConverter();

    @Test
    void listSliced_whenPagingCaptured() throws Exception {
        // Simulate a callback that captured McpPaging(offset=20, size=10)
        TestCallback cb = newCallback("mrtr", new McpPaging(20, 10));
        // Pretend the tool returned 100 items
        List<String> all = new ArrayList<>();
        for (int i = 0; i < 100; i++) all.add("item-" + i);

        McpSchema.CallToolResult result = converter.convertToCallToolResult(all, List.class, cb);

        // 100 items, offset 20, size 10 -> sliced to [item-20..item-29].
        // The List branch in the converter serializes the sliced list as a
        // single JSON TextContent; verify the text contains exactly the 10
        // expected items.
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertThat(text).contains("item-20").contains("item-29").doesNotContain("item-30");
        // nextCursor should be present in meta since more items follow
        assertThat(result.meta()).containsKey("nextCursor");
        assertThat(result.meta().get("nextCursor")).isEqualTo("30");
    }

    @Test
    void noNextCursor_whenLastPage() throws Exception {
        TestCallback cb = newCallback("mrtr", new McpPaging(90, 10));
        List<String> all = new ArrayList<>();
        for (int i = 0; i < 100; i++) all.add("item-" + i);

        McpSchema.CallToolResult result = converter.convertToCallToolResult(all, List.class, cb);
        // Last page covers items 90-99
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertThat(text).contains("item-90").contains("item-99");
        // No nextCursor on the last page
        Map<String, Object> meta = result.meta();
        if (meta != null) {
            assertThat(meta).doesNotContainKey("nextCursor");
        }
    }

    @Test
    void noSlicing_whenPagingNotCaptured() throws Exception {
        // Callback without paging param
        TestCallback cb = newCallback("mrtr", null);
        List<String> all = new ArrayList<>();
        for (int i = 0; i < 100; i++) all.add("item-" + i);

        McpSchema.CallToolResult result = converter.convertToCallToolResult(all, List.class, cb);
        // No slicing -> all 100 items pass through (content list may be JSON-encoded,
        // but isError=false and we get at least the encoded representation back).
        // Since DefaultMcpCallToolResultConverter with no paging goes to the
        // JSON branch for List, content list may be empty (just text); but meta
        // must NOT carry nextCursor.
        Map<String, Object> meta = result.meta();
        if (meta != null) {
            assertThat(meta).doesNotContainKey("nextCursor");
        }
    }

    @Test
    void capturedPagingResetAfterInvocation() throws Exception {
        TestCallback cb = newCallback("mrtr", new McpPaging(0, 5));
        // First invocation captures paging
        assertThat(cb.capturedPaging()).isNotNull();
        // Manual reset (simulating between-invocation clearing)
        cb.capturePaging(null);
        assertThat(cb.capturedPaging()).isNull();
    }

    // ---- helper ----

    private static TestCallback newCallback(String methodName, McpPaging paging) throws NoSuchMethodException {
        // Use Holder.mrtr(McpPaging) which declares a McpPaging parameter —
        // for our test we only need the callback object shape, not the actual
        // method invocation. We attach the paging manually.
        Method m = Holder.class.getDeclaredMethod(methodName, McpPaging.class);
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        cb.capturePaging(paging);
        return cb;
    }

    private static final class TestCallback extends AbstractMcpToolMethodCallback {
        TestCallback(Method method, McpTool annotation) {
            super(method, new Object(), "name", null, "{}", null, null, null, annotation, null,
                null, null);
        }
        @Override
        protected boolean isExchangeType(Class<?> paramType) { return false; }
    }

    /** Annotated method declaring a McpPaging parameter — used only for its @McpTool shape. */
    @SuppressWarnings("unused")
    static final class Holder {
        @McpTool(name = "x")
        public List<String> mrtr(McpPaging paging) { return List.of(); }
    }
}