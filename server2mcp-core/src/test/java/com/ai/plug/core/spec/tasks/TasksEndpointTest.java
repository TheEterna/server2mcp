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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TasksEndpointTest {

    @Test
    void register_thenGet_returnsRunningStatus() {
        var store = new InMemoryTaskStore();
        store.register(TaskTypes.TaskHandle.of("task-1"));
        var endpoint = new TasksEndpoint(store);
        Map<String, Object> body = endpoint.handleGet("task-1");
        assertThat(body).containsEntry("found", true);
        assertThat(body).containsKey("task");
    }

    @Test
    void getUnknownTask_returnsFoundFalse() {
        var endpoint = new TasksEndpoint(new InMemoryTaskStore());
        Map<String, Object> body = endpoint.handleGet("ghost");
        assertThat(body).containsEntry("found", false);
    }

    @Test
    void handleList_enumeratesAllTasks() {
        var store = new InMemoryTaskStore();
        store.register(TaskTypes.TaskHandle.of("t1"));
        store.register(TaskTypes.TaskHandle.of("t2"));
        var endpoint = new TasksEndpoint(store);
        Map<String, Object> body = endpoint.handleList();
        assertThat(body).containsEntry("count", 2);
        assertThat((java.util.List<?>) body.get("tasks")).hasSize(2);
    }

    @Test
    void cancel_transitionsToCancelled() {
        var store = new InMemoryTaskStore();
        store.register(TaskTypes.TaskHandle.of("t1"));
        var endpoint = new TasksEndpoint(store);
        Map<String, Object> body = endpoint.handleCancel("t1", "user request");
        assertThat(body).containsEntry("cancelled", true);
        Map<String, Object> getBody = endpoint.handleGet("t1");
        // task is a TaskStatus record, not a Map — assert via record accessor
        var task = (com.ai.plug.core.spec.tasks.TaskTypes.TaskStatus) getBody.get("task");
        assertThat(task.status()).isEqualTo(com.ai.plug.core.spec.tasks.TaskTypes.Status.CANCELLED);
        assertThat(task.message()).isEqualTo("user request");
    }

    @Test
    void cancel_unknownTask_returnsFalse() {
        var endpoint = new TasksEndpoint(new InMemoryTaskStore());
        Map<String, Object> body = endpoint.handleCancel("ghost", "x");
        assertThat(body).containsEntry("cancelled", false);
    }

    @Test
    void cancel_isIdempotent() {
        var store = new InMemoryTaskStore();
        store.register(TaskTypes.TaskHandle.of("t1"));
        var endpoint = new TasksEndpoint(store);
        endpoint.handleCancel("t1", "first");
        Map<String, Object> body = endpoint.handleCancel("t1", "second");
        assertThat(body).containsEntry("cancelled", true);
    }

    @Test
    void constructor_nullStoreRejected() {
        assertThatThrownBy(() -> new TasksEndpoint(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleGetJson_serializes() throws Exception {
        var store = new InMemoryTaskStore();
        store.register(TaskTypes.TaskHandle.of("t1"));
        var endpoint = new TasksEndpoint(store);
        String json = endpoint.handleGetJson("t1");
        assertThat(json).contains("\"found\":true");
        assertThat(json).contains("\"task\"");
    }

    @Test
    void handleListJson_serializes() throws Exception {
        var endpoint = new TasksEndpoint(new InMemoryTaskStore());
        String json = endpoint.handleListJson();
        assertThat(json).contains("\"count\":0");
        assertThat(json).contains("\"tasks\":[]");
    }

    @Test
    void handleCancelJson_serializes() throws Exception {
        var endpoint = new TasksEndpoint(new InMemoryTaskStore());
        String json = endpoint.handleCancelJson("ghost", "x");
        assertThat(json).contains("\"cancelled\":false");
    }
}