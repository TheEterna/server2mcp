/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.change;

import com.ai.plug.core.context.tool.IToolContext;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpToolChangeNotifierTest {

    @Test
    void requiresToolContext() {
        assertThatThrownBy(() -> McpToolChangeNotifier.forSync(null, () -> {}))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresAtLeastOneNotifier() {
        IToolContext ctx = proxyCtx(Map.of());
        assertThatThrownBy(() -> new McpToolChangeNotifier(ctx, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noChange_noNotification() {
        AtomicInteger calls = new AtomicInteger();
        IToolContext ctx = proxyCtx(Map.of("a", "alpha", "b", "beta"));
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);

        notifier.diffAndNotify();
        notifier.diffAndNotify();
        notifier.diffAndNotify();

        // First call fires (initial snapshot was -1/0), subsequent no-op
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void sizeChange_firesNotification() {
        AtomicInteger calls = new AtomicInteger();
        Map<String, Object> live = new HashMap<>(Map.of("a", "alpha"));
        IToolContext ctx = proxyCtxLive(live);
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);

        notifier.diffAndNotify(); // size 1 -> fires
        live.put("b", "beta");   // size 2
        notifier.diffAndNotify(); // fires
        notifier.diffAndNotify(); // no-op

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void valueChange_firesNotification() {
        AtomicInteger calls = new AtomicInteger();
        Map<String, Object> live = new HashMap<>(Map.of("a", "v1"));
        IToolContext ctx = proxyCtxLive(live);
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);

        notifier.diffAndNotify(); // initial -> fires
        live.put("a", "v2");     // value changed
        notifier.diffAndNotify(); // fires

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void resetSnapshot_forcesRefire() {
        AtomicInteger calls = new AtomicInteger();
        IToolContext ctx = proxyCtx(Map.of("a", "alpha"));
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);

        notifier.diffAndNotify();
        notifier.diffAndNotify(); // no-op
        assertThat(calls.get()).isEqualTo(1);

        notifier.resetSnapshot();
        notifier.diffAndNotify(); // forced refire
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void asyncNotifierFires() {
        AtomicInteger syncCalls = new AtomicInteger();
        AtomicInteger asyncCalls = new AtomicInteger();
        IToolContext ctx = proxyCtx(Map.of("a", "alpha"));
        McpToolChangeNotifier sync = McpToolChangeNotifier.forSync(ctx, syncCalls::incrementAndGet);
        McpToolChangeNotifier async = McpToolChangeNotifier.forAsync(ctx,
            () -> { asyncCalls.incrementAndGet(); return Mono.empty(); });

        sync.diffAndNotify();
        async.diffAndNotify();

        assertThat(syncCalls.get()).isEqualTo(1);
        assertThat(asyncCalls.get()).isEqualTo(1);
    }

    @Test
    void notifierException_swallowed() {
        IToolContext ctx = proxyCtx(Map.of("a", "alpha"));
        // Notifier throws — notifier must not crash the caller
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx,
            () -> { throw new RuntimeException("boom"); });

        // First call: notifier runs (throws inside, caught); subsequent
        // calls no-op because lastSize was already updated. Just verify it
        // doesn't propagate.
        notifier.diffAndNotify();
        notifier.diffAndNotify(); // should not throw
    }

    // ---- JDK proxy helpers ----

    private static IToolContext proxyCtx(Map<String, ?> snapshot) {
        return (IToolContext) Proxy.newProxyInstance(
            McpToolChangeNotifierTest.class.getClassLoader(),
            new Class<?>[]{IToolContext.class},
            (proxy, method, args) -> {
                if ("getRawTools".equals(method.getName())) {
                    return new HashMap<>(snapshot);
                }
                return null;
            });
    }

    private static IToolContext proxyCtxLive(Map<String, Object> live) {
        return (IToolContext) Proxy.newProxyInstance(
            McpToolChangeNotifierTest.class.getClassLoader(),
            new Class<?>[]{IToolContext.class},
            (proxy, method, args) -> {
                if ("getRawTools".equals(method.getName())) {
                    return live;
                }
                return null;
            });
    }
}