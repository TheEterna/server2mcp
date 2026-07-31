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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TaskTypes} — verifies the Tasks extension wire schema
 * (MCP protocol 2026-07-28 SEP-2663).
 */
class TaskTypesTest {

    private static final ObjectMapper M = new ObjectMapper();

    @Test
    void taskHandle_basic() {
        TaskTypes.TaskHandle handle = TaskTypes.TaskHandle.of("task-abc-123");
        assertThat(handle.taskId()).isEqualTo("task-abc-123");
        assertThat(handle.meta()).isNull();
    }

    @Test
    void taskHandle_withMeta() {
        TaskTypes.TaskHandle handle = TaskTypes.TaskHandle.of(
            "task-1", Map.of("priority", "high"));
        assertThat(handle.meta()).containsEntry("priority", "high");
    }

    @Test
    void taskHandle_blankIdRejected() {
        assertThatThrownBy(() -> TaskTypes.TaskHandle.of(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaskTypes.TaskHandle.of(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void taskStatus_pending() {
        TaskTypes.TaskStatus s = TaskTypes.TaskStatus.pending();
        assertThat(s.status()).isEqualTo(TaskTypes.Status.PENDING);
        assertThat(s.result()).isNull();
    }

    @Test
    void taskStatus_runningWithProgress() throws Exception {
        TaskTypes.TaskStatus s = TaskTypes.TaskStatus.running(0.5, "halfway");
        String json = M.writeValueAsString(s);
        assertThat(json).contains("\"status\":\"running\"");
        assertThat(json).contains("\"progress\":0.5");
        assertThat(json).contains("\"message\":\"halfway\"");
    }

    @Test
    void taskStatus_completedCarriesResult() throws Exception {
        Map<String, Object> resultPayload = Map.of("output", "hello world");
        TaskTypes.TaskStatus s = TaskTypes.TaskStatus.completed(resultPayload);
        String json = M.writeValueAsString(s);
        assertThat(json).contains("\"status\":\"completed\"");
        assertThat(json).contains("\"output\":\"hello world\"");
    }

    @Test
    void taskStatus_failedCarriesError() throws Exception {
        TaskTypes.TaskStatus s = TaskTypes.TaskStatus.failed(-32020, "header mismatch");
        String json = M.writeValueAsString(s);
        assertThat(json).contains("\"status\":\"failed\"");
        assertThat(json).contains("\"code\":-32020");
        assertThat(json).contains("\"message\":\"header mismatch\"");
    }

    @Test
    void taskStatus_terminalMustCarryResultOrError() {
        // Completed without result -> illegal
        assertThatThrownBy(() -> new TaskTypes.TaskStatus(
            TaskTypes.Status.COMPLETED, null, null, 1.0, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("terminal");
        // Failed without error -> illegal
        assertThatThrownBy(() -> new TaskTypes.TaskStatus(
            TaskTypes.Status.FAILED, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void taskStatus_bothResultAndErrorRejected() {
        TaskTypes.TaskError err = TaskTypes.TaskError.of(-1, "x");
        assertThatThrownBy(() -> new TaskTypes.TaskStatus(
            TaskTypes.Status.COMPLETED, "result", err, 1.0, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("both");
    }

    @Test
    void taskError_blankMessageRejected() {
        assertThatThrownBy(() -> TaskTypes.TaskError.of(-1, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void taskUpdate_basic() {
        TaskTypes.TaskUpdate update = TaskTypes.TaskUpdate.of("task-1", Map.of("pause", true));
        assertThat(update.taskId()).isEqualTo("task-1");
        assertThat(update.input()).isInstanceOf(Map.class);
    }

    @Test
    void taskUpdate_blankIdRejected() {
        assertThatThrownBy(() -> TaskTypes.TaskUpdate.of("", "anything"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}