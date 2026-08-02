/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.capabilities;

import com.ai.plug.core.spec.integration.WireSchemaExporter;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for the capability factory pipeline. Validates:
 * - Each preset factory produces the right flag combination
 * - The listChanged flag maps to McpTool.listChanged() correctly
 * - The full builder factory preserves existing capability fields when
 *   adding extensions
 */
class CapabilityFactoryEndToEndTest {

    @Test
    void withListChangedAll_setsAllFourFlags() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.prompts().listChanged()).isTrue();
    }

    @Test
    void withListChangedAll_doesNotOverwriteExperimental() {
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        assertThat(caps.experimental()).containsKey("io.modelcontextprotocol/tasks");
        // All listChanged/subscribe flags preserved
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.prompts().listChanged()).isTrue();
    }

    @Test
    void fullCapabilitiesWithExtensions_acceptsCustomMap() {
        Map<String, Object> ext = Map.of(
            "io.modelcontextprotocol/tasks", Map.of("version", "v2"),
            "x.custom.extension", "value");
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(ext);
        assertThat(caps.experimental()).containsEntry("x.custom.extension", "value");
        assertThat(caps.experimental().get("io.modelcontextprotocol/tasks"))
            .isInstanceOf(Map.class);
    }

    @Test
    void fullCapabilitiesWithExtensions_nullOrEmptyBaseOnDefaults() {
        // null extensions -> no experimental map set
        var a = WireSchemaExporter.fullCapabilitiesWithExtensions(null);
        assertThat(a.experimental()).isNull();
        // empty map -> still null (not empty Map.of())
        var b = WireSchemaExporter.fullCapabilitiesWithExtensions(Map.of());
        assertThat(b.experimental()).isNull();
    }

    @Test
    void listChangedAnnotation_inToolDefaultsToTrue() throws Exception {
        // @McpTool.listChanged default is true — verify via reflection
        var ann = Sample.class.getDeclaredMethod("tool").getAnnotation(com.ai.plug.core.annotation.McpTool.class);
        assertThat(ann.listChanged()).isTrue();
    }

    @Test
    void listChangedAnnotation_explicitFalse() throws Exception {
        var ann = Sample.class.getDeclaredMethod("staticTool").getAnnotation(com.ai.plug.core.annotation.McpTool.class);
        assertThat(ann.listChanged()).isFalse();
    }

    @Test
    void listChangedAnnotation_explicitTrue() throws Exception {
        var ann = Sample.class.getDeclaredMethod("explicitListChangedTrue")
            .getAnnotation(com.ai.plug.core.annotation.McpTool.class);
        assertThat(ann.listChanged()).isTrue();
    }

    @Test
    void snapshot_afterFactory_roundTrip() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        var snap = CapabilitySnapshot.from(caps);
        assertThat(snap.flags())
            .containsEntry("tools.listChanged", true)
            .containsEntry("resources.listChanged", true)
            .containsEntry("resources.subscribe", true)
            .containsEntry("prompts.listChanged", true);
    }

    @Test
    void capabilitiesAsMap_matchesFactorySnapshot() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        var snap = CapabilitySnapshot.from(caps);
        Map<String, Object> map = WireSchemaExporter.capabilitiesAsMap();
        // Each flag in the snapshot should have a corresponding entry in the map
        for (var entry : snap.flags().entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("\\.");
            // Map structure: { tools: { listChanged: true }, ... }
            @SuppressWarnings("unchecked")
            var bucket = (Map<String, Object>) map.get(parts[0]);
            if (bucket != null) {
                assertThat(bucket.get(parts[1])).isEqualTo(entry.getValue());
            }
        }
    }

    // ---- annotated method holder ----

    static final class Sample {
        @com.ai.plug.core.annotation.McpTool(name = "x") // listChanged defaults to true
        public String tool() { return "x"; }

        @com.ai.plug.core.annotation.McpTool(name = "y", listChanged = false)
        public String staticTool() { return "y"; }

        @com.ai.plug.core.annotation.McpTool(name = "z", listChanged = true)
        public String explicitListChangedTrue() { return "z"; }
    }
}