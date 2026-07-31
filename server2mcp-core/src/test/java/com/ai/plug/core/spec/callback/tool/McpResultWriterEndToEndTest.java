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
import com.ai.plug.core.spec.mrtr.MrtrTypes;
import com.ai.plug.core.spec.resulttype.McpResultWriter;
import com.ai.plug.core.spec.tasks.TaskTypes;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for the converter → writer chain.
 * Verifies that a tool method marked with {@link McpTool} resultType/ttlMs
 * hints, OR returning an MRTR InputRequiredResult / TaskHandle, is correctly
 * reflected in the wire JSON produced by {@link McpResultWriter}.
 */
class McpResultWriterEndToEndTest {

    private final DefaultMcpCallToolResultConverter converter = new DefaultMcpCallToolResultConverter();

    @Test
    void resultType_completeWritten() throws Exception {
        Method m = Holder.class.getDeclaredMethod("plain");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        McpSchema.CallToolResult result = converter.convertToCallToolResult(
            List.of(new McpSchema.TextContent("hi")), List.class, cb);
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(json).contains("\"resultType\":\"complete\"");
    }

    @Test
    void resultType_inputRequiredWritten() throws Exception {
        Method m = Holder.class.getDeclaredMethod("mrtr");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        McpSchema.CallToolResult result = converter.convertToCallToolResult(
            MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.RootsInputRequest.create()),
                "corr-1"),
            MrtrTypes.InputRequiredResult.class, cb);
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(json).contains("\"resultType\":\"input_required\"");
        assertThat(json).contains("\"inputRequests\"");
        assertThat(json).contains("\"requestState\":\"corr-1\"");
    }

    @Test
    void resultType_taskHandleWritten() throws Exception {
        Method m = Holder.class.getDeclaredMethod("task");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        McpSchema.CallToolResult result = converter.convertToCallToolResult(
            TaskTypes.TaskHandle.of("task-7"),
            TaskTypes.TaskHandle.class, cb);
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(json).contains("\"resultType\":\"complete\"");
        // taskHandle carried via structuredContent / meta — writer doesn't need
        // to know about TaskHandle specifically; the converter captures it in meta
        assertThat(result.meta()).containsEntry("taskHandle", "task-7");
    }

    @Test
    void cacheHintPropagates() throws Exception {
        Method m = Holder.class.getDeclaredMethod("cached");
        TestCallback cb = new TestCallback(m, m.getAnnotation(McpTool.class));
        McpSchema.CallToolResult result = converter.convertToCallToolResult(
            List.of(new McpSchema.TextContent("cached payload")), List.class, cb);
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        // Complete resultType from cacheWrapperKey default
        assertThat(json).contains("\"resultType\":\"complete\"");
        // Custom cacheWrapperKey respected
        assertThat(json).contains("\"myCache\"");
        assertThat(json).contains("\"ttlMs\":60000");
        assertThat(json).contains("\"cacheScope\":\"public\"");
    }

    // ---- minimal callback subclass ----

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
        public String plain() { return "hi"; }

        @McpTool(name = "x", resultType = "input_required")
        public MrtrTypes.InputRequiredResult mrtr() { return null; }

        @McpTool(name = "x")
        public TaskTypes.TaskHandle task() { return null; }

        @McpTool(name = "x", ttlMs = 60_000, cacheScope = "public", cacheWrapperKey = "myCache")
        public String cached() { return "hi"; }
    }
}