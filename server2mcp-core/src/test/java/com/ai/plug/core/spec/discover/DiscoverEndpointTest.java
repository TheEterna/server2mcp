/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.discover;

import com.ai.plug.core.spec.capabilities.ServerCapabilitiesFactory;
import com.ai.plug.core.spec.integration.WireSchemaExporter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscoverEndpointTest {

    @Test
    void handle_emitsRequiredFields() {
        var endpoint = new DiscoverEndpoint(
            "my-server", "1.0.0",
            ServerCapabilitiesFactory::withListChangedAll);
        Map<String, Object> body = endpoint.handle();
        assertThat(body).containsKey("protocolVersions");
        assertThat(body).containsKey("preferredVersion");
        assertThat(body).containsKey("serverInfo");
        assertThat(body).containsKey("capabilities");
        // serverInfo is an McpSchema.Implementation record, not a Map —
        // verify its identity fields directly via Jackson serialisation.
        assertThat(body.get("serverInfo"))
            .isInstanceOf(io.modelcontextprotocol.spec.McpSchema.Implementation.class);
        var impl = (io.modelcontextprotocol.spec.McpSchema.Implementation) body.get("serverInfo");
        assertThat(impl.name()).isEqualTo("my-server");
        assertThat(impl.version()).isEqualTo("1.0.0");
    }

    @Test
    void handleJson_parsesAsValidJson() throws Exception {
        var endpoint = new DiscoverEndpoint(
            "my-server", "1.0.0",
            ServerCapabilitiesFactory::withListChangedAll,
            () -> WireSchemaExporter.tasksExtension());
        String json = endpoint.handleJson();
        assertThat(json).contains("\"protocolVersions\"");
        assertThat(json).contains("\"preferredVersion\":\"2026-07-28\"");
        assertThat(json).contains("\"serverInfo\"");
        assertThat(json).contains("\"capabilities\"");
        assertThat(json).contains("\"io.modelcontextprotocol/tasks\"");
    }

    @Test
    void constructor_validatesInputs() {
        assertThatThrownBy(() -> new DiscoverEndpoint(
            "", "1.0.0", () -> null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiscoverEndpoint(
            "name", "", () -> null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiscoverEndpoint(
            "name", "1.0", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negotiate_returnsClientPreferred_whenSupported() {
        assertThat(DiscoverEndpoint.negotiate("2026-07-28"))
            .isEqualTo("2026-07-28");
        assertThat(DiscoverEndpoint.negotiate("2025-11-25"))
            .isEqualTo("2025-11-25");
    }

    @Test
    void negotiate_returnsServerPreferred_whenUnsupported() {
        assertThat(DiscoverEndpoint.negotiate("2030-01-01"))
            .isEqualTo("2026-07-28");
    }

    @Test
    void negotiate_returnsServerPreferred_whenBlank() {
        assertThat(DiscoverEndpoint.negotiate(null))
            .isEqualTo("2026-07-28");
        assertThat(DiscoverEndpoint.negotiate(""))
            .isEqualTo("2026-07-28");
    }
}