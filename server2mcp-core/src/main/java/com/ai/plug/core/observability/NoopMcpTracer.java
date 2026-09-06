/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.observability;

import java.util.Map;

/**
 * No-op implementation of {@link McpTracer}. Used by default when no
 * other {@code McpTracer} bean is registered — every method is a
 * constant-time no-op, so the framework's tracing calls are
 * effectively free.
 */
public final class NoopMcpTracer implements McpTracer {

    @Override
    public Span startSpan(String name) {
        return NoopSpan.INSTANCE;
    }

    @Override
    public Span startSpan(String name, Map<String, Object> attributes) {
        return NoopSpan.INSTANCE;
    }

    private static final class NoopSpan implements Span {
        static final NoopSpan INSTANCE = new NoopSpan();

        @Override public void setAttribute(String key, Object value) { }
        @Override public void recordException(Throwable t) { }
        @Override public void end(Throwable error) { }
        @Override public void end() { }
    }
}
