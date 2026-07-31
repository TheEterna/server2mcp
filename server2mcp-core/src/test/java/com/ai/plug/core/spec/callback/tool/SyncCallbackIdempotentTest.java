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
import com.ai.plug.core.spec.dedup.IdempotentCache;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests verifying that {@link SyncMcpToolMethodCallback} correctly applies
 * the {@link IdempotentCache} when the underlying method is annotated
 * {@code @McpTool(idempotentHint=true)}.
 */
class SyncCallbackIdempotentTest {

    @Test
    void idempotentHint_true_withCache_secondCallReturnsCached() throws Exception {
        // Method is annotated with idempotentHint=true (explicit)
        Method m = Holder.class.getDeclaredMethod("count");
        assertThat(m.getAnnotation(McpTool.class).idempotentHint()).isTrue();

        Holder freshBean = new Holder();
        IdempotentCache cache = new IdempotentCache(60_000);
        SyncMcpToolMethodCallback cb = new SyncMcpToolMethodCallback.Builder()
            .method(m)
            .bean(freshBean)
            .name("count")
            .inputSchema("{}")
            .toolAnnotation(m.getAnnotation(McpTool.class))
            .converter(new DefaultMcpCallToolResultConverter())
            .idempotentCache(cache)
            .build();

        // First call — should execute and cache.
        String fp1 = cache.fingerprint("count", Map.of());
        McpSchema.CallToolResult r1 = cb.apply(null,
            new McpSchema.CallToolRequest("count", Map.of()));
        // Sanity: cache should now contain this fp
        assertThat(cache.contains(fp1))
            .as("r1 should populate the cache; size=%d", cache.size())
            .isTrue();

        // Second call — should hit cache, NOT execute.
        McpSchema.CallToolResult r2 = cb.apply(null,
            new McpSchema.CallToolRequest("count", Map.of()));
        // Both r1 and r2 should carry the JSON-quoted "first"
        assertThat(((McpSchema.TextContent) r1.content().get(0)).text()).isEqualTo("\"first\"");
        assertThat(((McpSchema.TextContent) r2.content().get(0)).text()).isEqualTo("\"first\"");
        // The bean counter only ticks once
        assertThat(freshBean.calls.get())
            .as("r2 should have hit the cache, leaving the bean counter at 1")
            .isEqualTo(1);
    }

    @Test
    void idempotentHint_false_cacheIgnored() throws Exception {
        Method m = Holder.class.getDeclaredMethod("notIdempotent");
        assertThat(m.getAnnotation(McpTool.class).idempotentHint()).isFalse();

        Holder freshBean = new Holder();
        IdempotentCache cache = new IdempotentCache(60_000);
        SyncMcpToolMethodCallback cb = new SyncMcpToolMethodCallback.Builder()
            .method(m)
            .bean(freshBean)
            .name("notIdempotent")
            .inputSchema("{}")
            .toolAnnotation(m.getAnnotation(McpTool.class))
            .converter(new DefaultMcpCallToolResultConverter())
            .idempotentCache(cache)
            .build();

        // Each call should execute fresh — cache is configured but
        // idempotentHint=false short-circuits it.
        McpSchema.CallToolResult r1 = cb.apply(null,
            new McpSchema.CallToolRequest("notIdempotent", Map.of()));
        McpSchema.CallToolResult r2 = cb.apply(null,
            new McpSchema.CallToolRequest("notIdempotent", Map.of()));
        assertThat(((McpSchema.TextContent) r1.content().get(0)).text()).isEqualTo("\"a\"");
        assertThat(((McpSchema.TextContent) r2.content().get(0)).text()).isEqualTo("\"b\"");
        assertThat(freshBean.calls.get()).isEqualTo(2);
    }

    @Test
    void idempotentHint_true_withoutCache_executesAlways() throws Exception {
        Method m = Holder.class.getDeclaredMethod("count");
        Holder freshBean = new Holder();
        IdempotentCache cache = null; // explicit: no cache
        SyncMcpToolMethodCallback cb = new SyncMcpToolMethodCallback.Builder()
            .method(m)
            .bean(freshBean)
            .name("count")
            .inputSchema("{}")
            .toolAnnotation(m.getAnnotation(McpTool.class))
            .converter(new DefaultMcpCallToolResultConverter())
            .idempotentCache(cache) // null — no cache
            .build();

        // Each call executes fresh (Holder increments state)
        cb.apply(null, new McpSchema.CallToolRequest("count", Map.of()));
        McpSchema.CallToolResult r2 = cb.apply(null,
            new McpSchema.CallToolRequest("count", Map.of()));
        assertThat(((McpSchema.TextContent) r2.content().get(0)).text()).isEqualTo("\"second\"");
        assertThat(freshBean.calls.get()).isEqualTo(2);
    }

    /** Shared state for verifying call counts across tests. */
    private static final class State {
        final AtomicInteger calls = new AtomicInteger();
    }
    private static final ThreadLocal<State> STATE = new ThreadLocal<>();
    @SuppressWarnings("unused")
    private static State state() {
        State s = STATE.get();
        if (s == null) {
            s = new State();
            STATE.set(s);
        }
        s.calls.set(0);
        return s;
    }

    /** Bean with counter state — verifies each call actually executes. */
    static final class Holder {
        final AtomicInteger calls = new AtomicInteger();

        @McpTool(name = "count", idempotentHint = true)
        public String count() {
            int n = calls.incrementAndGet();
            return n == 1 ? "first" : "second";
        }

        @McpTool(name = "notIdempotent", idempotentHint = false)
        public String notIdempotent() {
            int n = calls.incrementAndGet();
            return n == 1 ? "a" : "b";
        }
    }
}