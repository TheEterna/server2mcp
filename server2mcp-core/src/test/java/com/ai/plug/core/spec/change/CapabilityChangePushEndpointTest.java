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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityChangePushEndpointTest {

    @Test
    void handlePush_firesOnChange() {
        AtomicInteger calls = new AtomicInteger();
        IToolContext ctx = proxyCtx();
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);
        CapabilityChangePushEndpoint endpoint = new CapabilityChangePushEndpoint(notifier);

        int fired = endpoint.handlePush();
        // First call fires (initial snapshot -1, fires once)
        assertThat(fired).isEqualTo(1);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void handlePush_noOpWhenUnchanged() {
        AtomicInteger calls = new AtomicInteger();
        IToolContext ctx = proxyCtx();
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);
        CapabilityChangePushEndpoint endpoint = new CapabilityChangePushEndpoint(notifier);

        // First call fires, second doesn't (no change)
        endpoint.handlePush();
        int fired = endpoint.handlePush();
        assertThat(fired).isZero();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void handleForcePush_firesOnce() {
        AtomicInteger calls = new AtomicInteger();
        IToolContext ctx = proxyCtx();
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);
        CapabilityChangePushEndpoint endpoint = new CapabilityChangePushEndpoint(notifier);

        // First call fires (initial snapshot); subsequent calls no-op
        // (no change). For true force-fire semantics regardless of snapshot,
        // use resetSnapshot() + diffAndNotify() — but for the default
        // endpoint contract, dedup is the expected behavior.
        endpoint.handleForcePush();
        endpoint.handleForcePush();
        endpoint.handleForcePush();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void constructor_nullNotifierRejected() {
        assertThatThrownBy(() -> new CapabilityChangePushEndpoint(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static IToolContext proxyCtx() {
        return (IToolContext) Proxy.newProxyInstance(
            CapabilityChangePushEndpointTest.class.getClassLoader(),
            new Class<?>[]{IToolContext.class},
            (proxy, method, args) -> {
                if ("getRawTools".equals(method.getName())) {
                    return java.util.Map.of();
                }
                return null;
            });
    }
}