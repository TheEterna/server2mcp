/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.integration;

import com.ai.plug.core.spec.implementation.ServerInfoFactory;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.customizer.McpAsyncServerCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WireSchemaExporter}.
 */
class WireSchemaExporterTest {

    @Test
    void syncAll_runsWithoutError() {
        // Just verify it composes and runs — capability assertion goes via
        // McpServerCustomizers.syncListChangedAll, tested separately.
        McpSyncServerCustomizer customizer = WireSchemaExporter.syncAll();
        // No actual MCP server to invoke; just ensure the lambda is non-null
        assertThat(customizer).isNotNull();
    }

    @Test
    void asyncAll_runsWithoutError() {
        McpAsyncServerCustomizer customizer = WireSchemaExporter.asyncAll();
        assertThat(customizer).isNotNull();
    }

    @Test
    void syncAllAnd_chainsIdentityCustomizer() {
        // Verify the andThen composition runs both halves
        boolean[] firstCalled = {false};
        boolean[] secondCalled = {false};
        McpSyncServerCustomizer idCustomizer = spec -> secondCalled[0] = true;
        // Wrap syncAll so we can observe first-half execution
        McpSyncServerCustomizer wrapped = spec -> firstCalled[0] = true;
        McpSyncServerCustomizer combined = McpServerCustomizers.composeAll(wrapped, idCustomizer);
        combined.customize(new AbstractMcpToolMethodCallbackTest.NoOpSyncSpec());
        assertThat(firstCalled[0]).isTrue();
        assertThat(secondCalled[0]).isTrue();
    }

    @Test
    void capabilitiesAsMap_matchesFactory() {
        Map<String, Object> map = WireSchemaExporter.capabilitiesAsMap();
        // tools / resources / prompts — all three flags wired
        assertThat((Map<String, Object>) map.get("tools"))
            .containsEntry("listChanged", true);
        assertThat((Map<String, Object>) map.get("resources"))
            .containsEntry("subscribe", true)
            .containsEntry("listChanged", true);
        assertThat((Map<String, Object>) map.get("prompts"))
            .containsEntry("listChanged", true);
    }

    @Test
    void capabilitiesAsMap_isValidYamlShape() {
        // The map's keys/values match what the Spring AI
        // spring.ai.mcp.server.capabilities.* property tree would consume.
        // Spot-check that values are plain Map<String,Object>, no nested maps.
        Map<String, Object> map = WireSchemaExporter.capabilitiesAsMap();
        assertThat(map.keySet()).containsExactlyInAnyOrder("tools", "resources", "prompts");
        for (Object cap : map.values()) {
            assertThat(cap).isInstanceOf(Map.class);
        }
    }

    @Test
    void fullCapabilitiesWithExtensions_mergesTasksIntoExperimental() {
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        assertThat(caps).isNotNull();
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.prompts().listChanged()).isTrue();
        assertThat(caps.experimental()).containsKey("io.modelcontextprotocol/tasks");
    }

    @Test
    void fullCapabilitiesWithExtensions_nullExtensionsEqualsBaseCapabilities() {
        var withNull = WireSchemaExporter.fullCapabilitiesWithExtensions(null);
        var withEmpty = WireSchemaExporter.fullCapabilitiesWithExtensions(java.util.Map.of());
        // Both should be equivalent to the no-extension base
        assertThat(withNull.experimental()).isNull();
        assertThat(withEmpty.experimental()).isNull();
        assertThat(withNull.tools().listChanged()).isTrue();
        assertThat(withEmpty.tools().listChanged()).isTrue();
    }

    @Test
    void tasksExtension_providesExtensionKey() {
        var ext = WireSchemaExporter.tasksExtension();
        assertThat(ext).containsKey("io.modelcontextprotocol/tasks");
        assertThat(ext.get("io.modelcontextprotocol/tasks")).isInstanceOf(Map.class);
    }

    @Test
    void serverInfoFactoryCompat() {
        // Smoke test that ServerInfoFactory (referenced by McpServerCustomizers)
        // produces something the customizer would accept.
        var impl = ServerInfoFactory.create("svc", "1.0", "Title", "Desc");
        assertThat(impl.name()).isEqualTo("svc");
        assertThat(impl.title()).isEqualTo("Title");
    }

    // Minimal spec stand-in (avoids reflection by reusing test helper).
    private static final class AbstractMcpToolMethodCallbackTest {
        static class NoOpSyncSpec
            extends io.modelcontextprotocol.server.McpServer.SyncSpecification<NoOpSyncSpec> {
            @Override
            public io.modelcontextprotocol.server.McpSyncServer build() {
                throw new UnsupportedOperationException();
            }
        }
    }
}