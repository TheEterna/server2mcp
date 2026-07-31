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

import com.ai.plug.core.spec.mrtr.MrtrTypes;
import com.ai.plug.core.spec.tasks.TaskTypes;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests verifying that {@link DefaultMcpCallToolResultConverter} recognizes
 * {@link MrtrTypes.InputRequiredResult} and {@link TaskTypes.TaskHandle} return
 * values from tool methods and converts them into well-formed CallToolResults.
 */
class MrtrTaskHandleReturnTest {

    private final DefaultMcpCallToolResultConverter converter = new DefaultMcpCallToolResultConverter();

    @Test
    void inputRequiredResult_isWrappedWithResultTypeMeta() {
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.RootsInputRequest.create(), MrtrTypes.ElicitationInputRequest.create(
                "Pick one", Map.of("type", "object"))),
            "corr-123");

        McpSchema.CallToolResult result = converter.convertToCallToolResult(irr, Object.class, null);

        assertThat(result).isNotNull();
        assertThat(result.isError()).isFalse();
        // Structured content preserved for downstream wire serialization
        assertThat(result.structuredContent()).isInstanceOf(MrtrTypes.InputRequiredResult.class);
        // Meta carries resultType + inputRequests for McpResultWriter compatibility
        assertThat(result.meta()).containsKey("resultType");
        assertThat(result.meta().get("resultType")).isEqualTo("input_required");
        assertThat(result.meta().get("inputRequests")).isInstanceOf(List.class);
        // requestState propagated to meta (no CallToolResult.requestState field
        // exists in SDK 2.0, so it lives in meta per our convention)
        assertThat(result.meta().get("requestState")).isEqualTo("corr-123");
    }

    @Test
    void inputRequiredResult_withoutRequestState_omitsIt() {
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.RootsInputRequest.create()));

        McpSchema.CallToolResult result = converter.convertToCallToolResult(irr, Object.class, null);

        assertThat(result.meta()).doesNotContainKey("requestState");
    }

    @Test
    void taskHandle_isWrappedWithHandleMeta() {
        TaskTypes.TaskHandle handle = TaskTypes.TaskHandle.of("task-abc-123");
        McpSchema.CallToolResult result = converter.convertToCallToolResult(handle, Object.class, null);

        assertThat(result).isNotNull();
        assertThat(result.isError()).isFalse();
        assertThat(result.structuredContent()).isInstanceOf(TaskTypes.TaskHandle.class);
        assertThat(result.meta()).containsEntry("taskHandle", "task-abc-123");
    }

    @Test
    void taskHandle_withMeta_propagatesToResult() {
        TaskTypes.TaskHandle handle = TaskTypes.TaskHandle.of(
            "task-1", Map.of("priority", "high", "ttl", 300));
        McpSchema.CallToolResult result = converter.convertToCallToolResult(handle, Object.class, null);

        assertThat(result.structuredContent()).isEqualTo(handle);
    }
}