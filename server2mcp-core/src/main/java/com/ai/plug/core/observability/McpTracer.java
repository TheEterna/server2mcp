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
 * SPI for emitting distributed-tracing signals from inside the
 * framework. Default impl: {@link NoopMcpTracer}, which does nothing.
 * Replace by registering a Spring bean of this type — for example, a
 * thin adapter that delegates to
 * {@code io.opentelemetry.api.trace.Tracer#spanBuilder(String)}.
 *
 * <h2>Why an SPI instead of a hard {@code opentelemetry-api} dep?</h2>
 * <p>The protocol-2026-07-28 layer already mints a W3C
 * {@code traceparent} on every response (see
 * {@code com.ai.plug.core.spec.meta.MetaUtils#ensureTraceparent}), so
 * the wire-format side of tracing is already covered. Real SDK
 * integration is per-deployment: some shops use OTel, others use Brave
 * (Zipkin), others have a homegrown logger. Forcing a dependency
 * would lock everyone in. SPI keeps the door open with no penalty.
 *
 * <h2>Span name conventions</h2>
 * <ul>
 *   <li>{@code mcp.jsonrpc.dispatch} — every JSON-RPC request envelope</li>
 *   <li>{@code mcp.tool.call} — every tool invocation</li>
 *   <li>{@code mcp.tool.list} — every {@code tools/list} snapshot</li>
 * </ul>
 *
 * <p>Attributes follow OTel semantic-conventions-style namespacing
 * (e.g. {@code mcp.method}, {@code mcp.tool.name},
 * {@code mcp.tenant.id}). When bridged to OTel, the names survive
 * unchanged.
 */
public interface McpTracer {

    /**
     * Start a new span. The returned handle is auto-closed when the
     * caller is done — see {@link Span#end()} and
     * {@link Span#end(Throwable)}.
     */
    Span startSpan(String name);

    /** Start a span with pre-computed attributes. */
    Span startSpan(String name, Map<String, Object> attributes);

    /** No-op tracer — default bean if nothing else is registered. */
    McpTracer NOOP = new NoopMcpTracer();

    /**
     * A single span's lifecycle handle. Implementations should be
     * safe to {@link #end()} exactly once; subsequent calls are
     * no-ops.
     */
    interface Span extends AutoCloseable {
        /** Add an attribute to the span (string, long, double, boolean). */
        void setAttribute(String key, Object value);

        /** Record an exception event. */
        void recordException(Throwable t);

        /** Mark the span as failed but finish it. */
        void end(Throwable error);

        /** End the span successfully. */
        void end();

        @Override
        default void close() {
            end();
        }
    }
}
