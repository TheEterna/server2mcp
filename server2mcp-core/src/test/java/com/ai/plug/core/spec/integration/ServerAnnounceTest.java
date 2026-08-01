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

import com.ai.plug.core.spec.capabilities.ServerCapabilitiesFactory;
import com.ai.plug.core.spec.implementation.ServerInfoFactory;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerAnnounceTest {

    @Test
    void builder_allFieldsSet() {
        ServerAnnounce ann = ServerAnnounce.builder()
            .info(ServerInfoFactory.create("my-mcp", "1.0", "My MCP", "Hello"))
            .capabilities(ServerCapabilitiesFactory.withListChangedAll())
            .extension("io.modelcontextprotocol/tasks", Map.of("version", "draft"))
            .build();

        assertThat(ann.serverInfo().name()).isEqualTo("my-mcp");
        assertThat(ann.capabilities().tools().listChanged()).isTrue();
        assertThat(ann.extensions()).containsKey("io.modelcontextprotocol/tasks");
    }

    @Test
    void minimal_factory_shorthand() {
        ServerAnnounce ann = ServerAnnounce.minimal("svc", "2.0");
        assertThat(ann.serverInfo().name()).isEqualTo("svc");
        assertThat(ann.serverInfo().version()).isEqualTo("2.0");
        // Default = listChanged all
        assertThat(ann.capabilities().tools().listChanged()).isTrue();
        assertThat(ann.extensions()).isEmpty();
    }

    @Test
    void extensionsImmutable() {
        ServerAnnounce ann = ServerAnnounce.builder()
            .info(ServerInfoFactory.create("svc", "1.0"))
            .capabilities(ServerCapabilitiesFactory.withToolsListChanged())
            .extension("k", "v")
            .build();
        // Defensive copy — extensions cannot be mutated after construction
        assertThat(ann.extensions()).isUnmodifiable();
    }

    @Test
    void toJson_serializesAllFields() throws Exception {
        ServerAnnounce ann = ServerAnnounce.builder()
            .info(ServerInfoFactory.create("svc", "1.0"))
            .capabilities(ServerCapabilitiesFactory.withListChangedAll())
            .extension("io.modelcontextprotocol/tasks", "draft")
            .build();
        String json = ann.toJson();
        assertThat(json).contains("\"name\":\"svc\"");
        assertThat(json).contains("\"version\":\"1.0\"");
        assertThat(json).contains("\"tools\"");
        assertThat(json).contains("\"listChanged\":true");
        assertThat(json).contains("\"io.modelcontextprotocol/tasks\"");
    }

    @Test
    void builder_requiresInfo() {
        assertThatThrownBy(() -> ServerAnnounce.builder()
            .capabilities(ServerCapabilitiesFactory.withToolsListChanged())
            .build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("info is required");
    }

    @Test
    void builder_requiresCapabilities() {
        assertThatThrownBy(() -> ServerAnnounce.builder()
            .info(ServerInfoFactory.create("svc", "1.0"))
            .build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("capabilities is required");
    }

    @Test
    void constructor_nullInfoRejected() {
        assertThatThrownBy(() -> new ServerAnnounce(
            null,
            ServerCapabilitiesFactory.withToolsListChanged(),
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}