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

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the McpToolChangeNotifier notifyNow() / onApplicationReady() path.
 */
class McpToolChangeNotifierPushTest {

    @Test
    void notifyNow_aliasesDiffAndNotify() {
        AtomicInteger calls = new AtomicInteger();
        IToolContext ctx = proxyCtx(Map.of("a", "alpha"));
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);

        notifier.notifyNow();
        notifier.notifyNow();
        notifier.notifyNow();

        // First call fires (initial snapshot), subsequent no-op
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void onApplicationReady_firesNotification() {
        AtomicInteger calls = new AtomicInteger();
        IToolContext ctx = proxyCtx(Map.of("a", "alpha"));
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);

        notifier.onApplicationReady();

        assertThat(calls.get())
            .as("onApplicationReady should have fired exactly one notification")
            .isEqualTo(1);
    }

    @Test
    void onApplicationReady_thenChange_firesSecondNotification() {
        AtomicInteger calls = new AtomicInteger();
        java.util.HashMap<String, Object> live = new java.util.HashMap<>(Map.of("a", "alpha"));
        IToolContext ctx = proxyCtxLive(live);
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);

        notifier.onApplicationReady(); // fires (initial snapshot)
        live.put("b", "beta");       // change
        notifier.notifyNow();         // should fire again
        notifier.notifyNow();         // no-op

        assertThat(calls.get()).isEqualTo(2);
    }

    // ---- JDK proxy helpers ----

    private static IToolContext proxyCtx(Map<String, ?> snapshot) {
        return (IToolContext) Proxy.newProxyInstance(
            McpToolChangeNotifierPushTest.class.getClassLoader(),
            new Class<?>[]{IToolContext.class},
            (proxy, method, args) -> {
                if ("getRawTools".equals(method.getName())) {
                    return new java.util.HashMap<>(snapshot);
                }
                return null;
            });
    }

    private static IToolContext proxyCtxLive(java.util.Map<String, Object> live) {
        return (IToolContext) Proxy.newProxyInstance(
            McpToolChangeNotifierPushTest.class.getClassLoader(),
            new Class<?>[]{IToolContext.class},
            (proxy, method, args) -> {
                if ("getRawTools".equals(method.getName())) {
                    return live;
                }
                return null;
            });
    }
}