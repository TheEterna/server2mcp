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
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies OpenTelemetry trace context propagation per SEP-414. When a
 * {@code tools/call} request carries traceparent / tracestate / baggage in its
 * {@code _meta}, the framework forwards these to the response's {@code _meta}.
 */
class McpTelemetryForwardTest {

    private final DefaultMcpCallToolResultConverter converter = new DefaultMcpCallToolResultConverter();

    @Test
    void traceparentForwardedToMeta() throws Exception {
        Method m = Holder.class.getDeclaredMethod("tool");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        // Simulate the callback having captured a request with traceparent
        cb.captureRequest(new McpSchema.CallToolRequest("tool",
            Map.of("x", 1),
            Map.of("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01")));

        McpSchema.CallToolResult result = converter.convertToCallToolResult(
            List.of(new McpSchema.TextContent("hi")), List.class, cb);

        // traceparent should appear in the response meta
        Map<String, Object> meta = result.meta();
        assertThat(meta).containsKey("traceparent");
        assertThat(meta.get("traceparent"))
            .isEqualTo("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
    }

    @Test
    void allThreeOtelKeysForwarded() throws Exception {
        Method m = Holder.class.getDeclaredMethod("tool");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        cb.captureRequest(new McpSchema.CallToolRequest("tool",
            Map.of("x", 1),
            Map.of(
                "traceparent", "00-x-x-01",
                "tracestate", "rojo=00f067aa0ba902b7",
                "baggage", "userId=alice"
            )));

        McpSchema.CallToolResult result = converter.convertToCallToolResult(
            List.of(new McpSchema.TextContent("ok")), List.class, cb);

        Map<String, Object> meta = result.meta();
        assertThat(meta).containsEntry("traceparent", "00-x-x-01");
        assertThat(meta).containsEntry("tracestate", "rojo=00f067aa0ba902b7");
        assertThat(meta).containsEntry("baggage", "userId=alice");
    }

    @Test
    void noOtelKeys_noTraceInjected() throws Exception {
        Method m = Holder.class.getDeclaredMethod("tool");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        cb.captureRequest(new McpSchema.CallToolRequest("tool",
            Map.of("x", 1), Map.of("unrelated", "key")));

        McpSchema.CallToolResult result = converter.convertToCallToolResult(
            List.of(new McpSchema.TextContent("ok")), List.class, cb);

        Map<String, Object> meta = result.meta();
        assertThat(meta).doesNotContainKey("traceparent");
        assertThat(meta).doesNotContainKey("tracestate");
        assertThat(meta).doesNotContainKey("baggage");
    }

    @Test
    void noRequest_capturedEmpty_noCrash() throws Exception {
        Method m = Holder.class.getDeclaredMethod("tool");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        // No captureRequest() call — currentRequest is null
        McpSchema.CallToolResult result = converter.convertToCallToolResult(
            List.of(new McpSchema.TextContent("ok")), List.class, cb);
        // No crash, no trace in meta
        Map<String, Object> meta = result.meta();
        if (meta != null) {
            assertThat(meta).doesNotContainKey("traceparent");
        }
    }

    // ---- helper ----

    private static final class TestCallback extends AbstractMcpToolMethodCallback {
        TestCallback(Method method, McpTool annotation) {
            super(method, new Object(), "name", null, "{}", null, null, null, annotation, null,
                null, null);
        }
        @Override
        protected boolean isExchangeType(Class<?> paramType) { return false; }
    }

    static final class Holder {
        @McpTool(name = "x")
        public String tool() { return "ok"; }
    }
}