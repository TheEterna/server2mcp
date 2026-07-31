/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.capabilities;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerCapabilitiesFactory}. Verifies that all factory
 * variants produce a {@link McpSchema.ServerCapabilities} with the expected
 * listChanged / subscribe flags wired through the SDK 2.0 Builder.
 */
class ServerCapabilitiesFactoryTest {

    @Test
    void withListChangedAll_setsAllFlags() {
        McpSchema.ServerCapabilities caps = ServerCapabilitiesFactory.withListChangedAll();
        assertThat(caps.tools().listChanged()).isTrue();
        // ResourceCapabilities(subscribe, listChanged) — factory passed (true, true)
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
        assertThat(caps.prompts().listChanged()).isTrue();
    }

    @Test
    void withToolsListChanged_toolsOnly() {
        McpSchema.ServerCapabilities caps = ServerCapabilitiesFactory.withToolsListChanged();
        assertThat(caps.tools().listChanged()).isTrue();
        // prompts/resources not declared -> tools() / prompts() / resources() return null
        assertThat(caps.prompts()).isNull();
        assertThat(caps.resources()).isNull();
    }

    @Test
    void withResourcesListChanged_resourcesOnly() {
        McpSchema.ServerCapabilities caps = ServerCapabilitiesFactory.withResourcesListChanged();
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
        assertThat(caps.tools()).isNull();
        assertThat(caps.prompts()).isNull();
    }

    @Test
    void withPromptsListChanged_promptsOnly() {
        McpSchema.ServerCapabilities caps = ServerCapabilitiesFactory.withPromptsListChanged();
        assertThat(caps.prompts().listChanged()).isTrue();
        assertThat(caps.tools()).isNull();
        assertThat(caps.resources()).isNull();
    }

    @Test
    void withExperimental_nullMapStillSucceeds() {
        McpSchema.ServerCapabilities caps = ServerCapabilitiesFactory.withExperimental(null);
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.experimental()).isNull();
    }

    @Test
    void withExperimental_nonNullMapPassesThrough() {
        Map<String, Object> experimental = Map.of(
                "io.modelcontextprotocol/tasks", Map.of("version", "draft"),
                "x-custom", "value");
        McpSchema.ServerCapabilities caps = ServerCapabilitiesFactory.withExperimental(experimental);
        assertThat(caps.experimental()).isNotNull().containsKey("io.modelcontextprotocol/tasks");
        assertThat(caps.experimental().get("x-custom")).isEqualTo("value");
        // Other flags still wired
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
    }
}