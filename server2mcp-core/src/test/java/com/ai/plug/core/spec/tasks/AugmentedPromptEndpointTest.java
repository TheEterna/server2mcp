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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AugmentedPromptEndpointTest {

    @Test
    void add_thenList_returnsAll() {
        var store = new InMemoryAugmentedPromptStore();
        var endpoint = new AugmentedPromptEndpoint(store);
        store.add(AugmentedPrompt.of("t1", "assistant", "first prompt"));
        store.add(AugmentedPrompt.of("t1", "assistant", "second prompt"));
        Map<String, Object> body = endpoint.handleList("t1");
        assertThat(body).containsEntry("taskId", "t1");
        assertThat(body).containsEntry("count", 2);
        assertThat((List<?>) body.get("prompts")).hasSize(2);
    }

    @Test
    void list_unknownTask_returnsZero() {
        var endpoint = new AugmentedPromptEndpoint(new InMemoryAugmentedPromptStore());
        Map<String, Object> body = endpoint.handleList("ghost");
        assertThat(body).containsEntry("count", 0);
    }

    @Test
    void drain_returnsAndClearsPrompts() {
        var store = new InMemoryAugmentedPromptStore();
        var endpoint = new AugmentedPromptEndpoint(store);
        store.add(AugmentedPrompt.of("t1", "assistant", "first"));
        store.add(AugmentedPrompt.of("t1", "assistant", "second"));
        Map<String, Object> drained = endpoint.handleDrain("t1");
        assertThat(drained).containsEntry("drained", 2);
        // Second call returns 0
        Map<String, Object> second = endpoint.handleDrain("t1");
        assertThat(second).containsEntry("drained", 0);
    }

    @Test
    void list_isolatesAcrossTasks() {
        var store = new InMemoryAugmentedPromptStore();
        var endpoint = new AugmentedPromptEndpoint(store);
        store.add(AugmentedPrompt.of("t1", "assistant", "for t1"));
        store.add(AugmentedPrompt.of("t2", "assistant", "for t2"));
        Map<String, Object> body1 = endpoint.handleList("t1");
        Map<String, Object> body2 = endpoint.handleList("t2");
        assertThat(body1).containsEntry("count", 1);
        assertThat(body2).containsEntry("count", 1);
    }

    @Test
    void constructor_nullStoreRejected() {
        assertThatThrownBy(() -> new AugmentedPromptEndpoint(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_validatesInputs() {
        assertThatThrownBy(() -> AugmentedPrompt.of(null, "assistant", "x"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AugmentedPrompt.of("t1", null, "x"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AugmentedPrompt.of("t1", "assistant", null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AugmentedPrompt.of("t1", "assistant", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleListJson_serializes() throws Exception {
        var store = new InMemoryAugmentedPromptStore();
        var endpoint = new AugmentedPromptEndpoint(store);
        store.add(AugmentedPrompt.of("t1", "assistant", "halfway done"));
        String json = endpoint.handleListJson("t1");
        assertThat(json).contains("\"taskId\":\"t1\"");
        assertThat(json).contains("\"count\":1");
        assertThat(json).contains("\"halfway done\"");
        assertThat(json).contains("\"role\":\"assistant\"");
        assertThat(json).contains("\"promptId\"");
        assertThat(json).contains("\"timestamp\"");
    }

    @Test
    void handleDrainJson_serializes() throws Exception {
        var store = new InMemoryAugmentedPromptStore();
        var endpoint = new AugmentedPromptEndpoint(store);
        store.add(AugmentedPrompt.of("t1", "assistant", "x"));
        String json = endpoint.handleDrainJson("t1");
        assertThat(json).contains("\"drained\":1");
        assertThat(json).contains("\"prompts\"");
    }

    @Test
    void get_singlePromptById() {
        var store = new InMemoryAugmentedPromptStore();
        var endpoint = new AugmentedPromptEndpoint(store);
        AugmentedPrompt p = AugmentedPrompt.of("t1", "assistant", "first");
        store.add(p);
        var fetched = store.get(p.promptId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().content()).isEqualTo("first");
    }

    @Test
    void activeCount_tracksAcrossTasks() {
        var store = new InMemoryAugmentedPromptStore();
        store.add(AugmentedPrompt.of("t1", "assistant", "a"));
        store.add(AugmentedPrompt.of("t1", "assistant", "b"));
        store.add(AugmentedPrompt.of("t2", "assistant", "c"));
        assertThat(store.activeCount()).isEqualTo(3);
        store.drain("t1");
        assertThat(store.activeCount()).isEqualTo(1);
    }
}