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

class CapabilityChangePushSchedulerTest {

    @Test
    void wrap_returnsRunnableCallingDiffAndNotify() {
        AtomicInteger calls = new AtomicInteger();
        IToolContext ctx = proxyCtx();
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);

        Runnable r = CapabilityChangePushScheduler.wrap(notifier);
        r.run();
        r.run();
        r.run();

        // First call fires (initial snapshot -1); subsequent no-op
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void wrapForce_returnsRunnableCallingNotifyNow() {
        AtomicInteger calls = new AtomicInteger();
        IToolContext ctx = proxyCtx();
        McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(ctx, calls::incrementAndGet);

        Runnable r = CapabilityChangePushScheduler.wrapForce(notifier);
        r.run();
        r.run();

        // notifyNow calls diffAndNotify under the hood; same fire pattern
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void wrap_nullNotifierRejected() {
        assertThatThrownBy(() -> CapabilityChangePushScheduler.wrap(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wrapForce_nullNotifierRejected() {
        assertThatThrownBy(() -> CapabilityChangePushScheduler.wrapForce(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static IToolContext proxyCtx() {
        return (IToolContext) Proxy.newProxyInstance(
            CapabilityChangePushSchedulerTest.class.getClassLoader(),
            new Class<?>[]{IToolContext.class},
            (proxy, method, args) -> {
                if ("getRawTools".equals(method.getName())) {
                    return java.util.Map.of();
                }
                return null;
            });
    }
}