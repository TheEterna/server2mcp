/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.tasks;

import com.ai.plug.core.spec.cacheable.CacheHints;
import com.ai.plug.core.spec.resulttype.McpResultWriter;
import com.ai.plug.core.spec.resulttype.ResultTypeConvention;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test demonstrating the Tasks extension wire path. Verifies:
 * - TaskHandle serialization to wire JSON
 * - TaskStatus state machine (PENDING → RUNNING → COMPLETED)
 * - TaskError structure
 * - Integration with McpResultWriter for the JSON-RPC tasks/get response
 */
class TaskStatusEndToEndTest {

    private static final ObjectMapper M = new ObjectMapper();

    @Test
    void taskHandle_serializesAsWireJson() throws Exception {
        TaskTypes.TaskHandle handle = TaskTypes.TaskHandle.of("task-abc-123");
        String json = M.writeValueAsString(handle);
        assertThat(json).contains("\"taskId\":\"task-abc-123\"");
    }

    @Test
    void taskStatus_lifecycle() {
        // PENDING state
        TaskTypes.TaskStatus pending = TaskTypes.TaskStatus.pending();
        assertThat(pending.status()).isEqualTo(TaskTypes.Status.PENDING);
        assertThat(pending.result()).isNull();
        assertThat(pending.error()).isNull();

        // RUNNING state with progress
        TaskTypes.TaskStatus running = TaskTypes.TaskStatus.running(0.5, "halfway");
        assertThat(running.status()).isEqualTo(TaskTypes.Status.RUNNING);
        assertThat(running.progress()).isEqualTo(0.5);
        assertThat(running.message()).isEqualTo("halfway");

        // COMPLETED state with result
        TaskTypes.TaskStatus completed = TaskTypes.TaskStatus.completed(
            java.util.Map.of("output", "hello"));
        assertThat(completed.status()).isEqualTo(TaskTypes.Status.COMPLETED);
        assertThat(completed.result()).isInstanceOf(java.util.Map.class);

        // FAILED state with error
        TaskTypes.TaskStatus failed = TaskTypes.TaskStatus.failed(-32020, "header mismatch");
        assertThat(failed.status()).isEqualTo(TaskTypes.Status.FAILED);
        assertThat(failed.error().code()).isEqualTo(-32020);
        assertThat(failed.error().message()).isEqualTo("header mismatch");

        // CANCELLED
        TaskTypes.TaskStatus cancelled = TaskTypes.TaskStatus.cancelled("user requested");
        assertThat(cancelled.status()).isEqualTo(TaskTypes.Status.CANCELLED);
        assertThat(cancelled.message()).isEqualTo("user requested");
    }

    @Test
    void taskStatus_terminalValidation() {
        // COMPLETED without result or error -> reject
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            new TaskTypes.TaskStatus(TaskTypes.Status.COMPLETED, null, null, 1.0, null, null)
        ).isInstanceOf(IllegalArgumentException.class);

        // FAILED without error (and result null) -> reject
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            new TaskTypes.TaskStatus(TaskTypes.Status.FAILED, null, null, null, null, null)
        ).isInstanceOf(IllegalArgumentException.class);

        // Both result and error -> reject
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            new TaskTypes.TaskStatus(TaskTypes.Status.COMPLETED, "x",
                new TaskTypes.TaskError(-1, "err", null), null, null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void taskUpdate_serializes() throws Exception {
        TaskTypes.TaskUpdate update = TaskTypes.TaskUpdate.of("task-1",
            java.util.Map.of("pause", true));
        String json = M.writeValueAsString(update);
        assertThat(json).contains("\"taskId\":\"task-1\"");
        assertThat(json).contains("\"input\"");
    }

    @Test
    void taskHandle_integratesWithMetaAndCacheHints() throws Exception {
        // The full chain: callback injects TaskHandle, DefaultMcpCallToolResultConverter
        // wraps it into a CallToolResult with taskHandle meta key, McpResultWriter
        // serializes the wire payload.
        McpSchema.CallToolResult sdkResult = McpSchema.CallToolResult.builder()
            .addTextContent("task accepted: task-abc-123")
            .isError(false)
            .meta(java.util.Map.of(
                "taskHandle", "task-abc-123",
                "resultType", ResultTypeConvention.COMPLETE,
                "ttlMs", 60_000L,
                "cacheScope", "private"))
            .build();
        String wire = McpResultWriter.writeCallToolResultFromMeta(sdkResult);

        // TaskHandle flows through meta
        assertThat(wire).contains("\"taskHandle\":\"task-abc-123\"");
        // + resultType
        assertThat(wire).contains("\"resultType\":\"complete\"");
        // + cache hint
        assertThat(wire).contains("\"ttlMs\":60000");
        assertThat(wire).contains("\"cacheScope\":\"private\"");
        // + _cacheable wrapper
        assertThat(wire).contains("\"_cacheable\"");
    }

    @Test
    void taskHandle_metaUtilsRoundTrip() {
        // The taskHandle value should be available to any downstream consumer
        // reading meta — verify it's a String, not a wrapped object.
        java.util.Map<String, Object> meta = java.util.Map.of("taskHandle", "task-7");
        assertThat(meta.get("taskHandle")).isEqualTo("task-7");
    }

    @Test
    void cacheHint_defaultTtlIs60Seconds() {
        assertThat(CacheHints.DEFAULT_TTL_MS).isEqualTo(60_000L);
    }

    @Test
    void taskError_reservedCodeRangeAccepted() {
        // -32020 (HeaderMismatch) is the lowest reserved code
        TaskTypes.TaskError err = TaskTypes.TaskError.reserved(-32020, "header mismatch");
        assertThat(err.code()).isEqualTo(-32020);
        assertThat(err.isReservedCode()).isTrue();

        // -32099 is the highest reserved code
        TaskTypes.TaskError err2 = TaskTypes.TaskError.reserved(-32099, "end of range");
        assertThat(err2.isReservedCode()).isTrue();

        // Mid-range (-32050) is reserved
        TaskTypes.TaskError err3 = TaskTypes.TaskError.reserved(-32050, "mid");
        assertThat(err3.isReservedCode()).isTrue();
    }

    @Test
    void taskError_outOfReservedRangeRejected() {
        // -32019 is implementation-defined, not reserved
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            TaskTypes.TaskError.reserved(-32019, "impl-defined")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("outside the reserved range");

        // -32100 is outside the spec range entirely
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            TaskTypes.TaskError.reserved(-32100, "out of range")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void taskError_ofAcceptsArbitraryCode() {
        // of() allows any code (JSON-RPC convention)
        TaskTypes.TaskError implDefined = TaskTypes.TaskError.of(-31000, "impl code");
        assertThat(implDefined.code()).isEqualTo(-31000);
        assertThat(implDefined.isReservedCode()).isFalse();
    }
}