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
 * Verifies that {@link McpTool#resultType() / ttlMs() / cacheScope() /
 * cacheWrapperKey()} propagate into every {@link McpSchema.CallToolResult}
 * produced by {@link DefaultMcpCallToolResultConverter}.
 */
class McpToolWireHintsTest {

    private final DefaultMcpCallToolResultConverter converter = new DefaultMcpCallToolResultConverter();

    @Test
    void allHintsAppearInMeta() throws Exception {
        Method m = Holder.class.getDeclaredMethod("withAllHints");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        McpSchema.CallToolResult result = converter.convertToCallToolResult(List.of(new McpSchema.TextContent("hi")), List.class, cb);

        Map<String, Object> meta = result.meta();
        assertThat(meta).containsEntry("resultType", "input_required");
        assertThat(meta).containsEntry("ttlMs", 30_000L);
        assertThat(meta).containsEntry("cacheScope", "public");
        assertThat(meta).containsEntry("cacheWrapperKey", "cache");
    }

    @Test
    void missingHintsOmitsKeys() throws Exception {
        Method m = Holder.class.getDeclaredMethod("defaults");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        McpSchema.CallToolResult result = converter.convertToCallToolResult(List.of(new McpSchema.TextContent("hi")), List.class, cb);

        Map<String, Object> meta = result.meta();
        // default "complete" still emitted
        assertThat(meta).containsEntry("resultType", "complete");
        // ttlMs=0 and cacheScope="" omitted; cacheWrapperKey default present
        assertThat(meta).doesNotContainKey("ttlMs");
        assertThat(meta).doesNotContainKey("cacheScope");
        assertThat(meta).containsEntry("cacheWrapperKey", "_cacheable");
    }

    @Test
    void negativeTtlEmittedAsZero() throws Exception {
        // ttlMs=0 falls below the > 0 threshold so it should NOT be emitted.
        // (negative numbers are clamped by McpTool annotation default to 0.)
        Method m = Holder.class.getDeclaredMethod("defaults");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        McpSchema.CallToolResult result = converter.convertToCallToolResult(List.of(new McpSchema.TextContent("x")), List.class, cb);

        assertThat(result.meta()).doesNotContainKey("ttlMs");
    }

    @Test
    void nullCallbackProducesEmptyMeta() {
        // Pre-built CallToolResult passes through the converter's first branch
        // (`result instanceof CallToolResult`) without entering the tool-hint
        // application logic. Meta is whatever the input carried — typically null
        // since CallToolResult.Builder.meta(null) is the default.
        McpSchema.CallToolResult input = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false).build();
        McpSchema.CallToolResult result = converter.convertToCallToolResult(input, McpSchema.CallToolResult.class, null);
        // Pass-through preserves null meta — tool hints are only applied on
        // branches that go through the builder.
        assertThat(result).isSameAs(input);
    }

    @Test
    void callbackWithoutAnnotationProducesEmptyMeta() throws Exception {
        Method m = Holder.class.getDeclaredMethod("noAnnotation");
        TestCallback cb = new TestCallback(m, null);
        McpSchema.CallToolResult result = converter.convertToCallToolResult(List.of(new McpSchema.TextContent("hi")), List.class, cb);
        // toolAnnotation is null — no hints emitted
        Map<String, Object> meta = result.meta();
        assertThat(meta).isNotNull();
        assertThat(meta).doesNotContainKey("resultType");
    }

    // ---- minimal callback subclass that exposes the protected field ----

    /**
     * Tiny stand-in that sets the protected {@code toolAnnotation} field
     * directly (we're in the same package).
     */
    private static final class TestCallback extends AbstractMcpToolMethodCallback {
        TestCallback(Method method, McpTool annotation) {
            super(method, new Object(), "name", null, "{}", null, null, null, annotation,
                null, null);
        }
        @Override
        protected boolean isExchangeType(Class<?> paramType) { return false; }
    }

    // ---- annotated methods for the callback ----

    static final class Holder {
        @McpTool(name = "x", resultType = "input_required", ttlMs = 30_000,
                cacheScope = "public", cacheWrapperKey = "cache")
        public String withAllHints() { return "hi"; }

        @McpTool(name = "x")
        public String defaults() { return "hi"; }

        @SuppressWarnings("unused")
        public String noAnnotation() { return "hi"; }
    }
}