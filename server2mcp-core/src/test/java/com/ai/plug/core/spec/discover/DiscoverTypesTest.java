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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DiscoverTypes} — verifies the server/discover wire schema
 * (MCP protocol 2026-07-28 SEP-2575).
 */
class DiscoverTypesTest {

    private static final ObjectMapper M = new ObjectMapper();

    @Test
    void discoverResult_minimal() throws Exception {
        DiscoverTypes.DiscoverResult r = DiscoverTypes.DiscoverResult.of(
            List.of("2025-11-25", "2026-07-28"),
            "2026-07-28",
            DiscoverTypes.ServerIdentity.of("svc", "1.0.0"));

        String json = M.writeValueAsString(r);
        assertThat(json).contains("\"protocolVersions\":[\"2025-11-25\",\"2026-07-28\"]");
        assertThat(json).contains("\"preferredVersion\":\"2026-07-28\"");
        assertThat(json).contains("\"serverInfo\"");
        assertThat(json).contains("\"name\":\"svc\"");
        assertThat(json).contains("\"version\":\"1.0.0\"");
    }

    @Test
    void discoverResult_preferredVersionMustBeInList() {
        assertThatThrownBy(() -> DiscoverTypes.DiscoverResult.of(
            List.of("2025-11-25"),
            "2026-07-28",
            DiscoverTypes.ServerIdentity.of("svc", "1.0")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2026-07-28");
    }

    @Test
    void discoverResult_emptyProtocolVersionsRejected() {
        assertThatThrownBy(() -> DiscoverTypes.DiscoverResult.of(
            List.of(),
            "2026-07-28",
            DiscoverTypes.ServerIdentity.of("svc", "1.0")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void discoverResult_nullServerInfoRejected() {
        assertThatThrownBy(() -> new DiscoverTypes.DiscoverResult(
            List.of("2026-07-28"), "2026-07-28", null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serverIdentity_blankNameRejected() {
        assertThatThrownBy(() -> DiscoverTypes.ServerIdentity.of("", "1.0"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscoverTypes.ServerIdentity.of("svc", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serverIdentity_withTitleAndDescription() {
        DiscoverTypes.ServerIdentity si = DiscoverTypes.ServerIdentity.of(
            "svc", "2.0", "My MCP", "Long description");
        assertThat(si.title()).isEqualTo("My MCP");
        assertThat(si.description()).isEqualTo("Long description");
    }

    @Test
    void capabilities_allWithListChanged() throws Exception {
        DiscoverTypes.Capabilities caps = DiscoverTypes.Capabilities.allWithListChanged();
        String json = M.writeValueAsString(caps);
        assertThat(json).contains("\"tools\"");
        assertThat(json).contains("\"listChanged\":true");
        assertThat(json).contains("\"subscribe\":true");
    }

    @Test
    void capabilities_withExtensions_advertisesTasks() throws Exception {
        Map<String, Object> extensions = Map.of(
            "io.modelcontextprotocol/tasks", Map.of("version", "draft"));
        DiscoverTypes.Capabilities caps = DiscoverTypes.Capabilities.withExtensions(extensions);
        String json = M.writeValueAsString(caps);
        assertThat(json).contains("\"extensions\"");
        assertThat(json).contains("\"io.modelcontextprotocol/tasks\"");
    }

    @Test
    void capabilities_emptyAllBucketsRejected() {
        assertThatThrownBy(() -> new DiscoverTypes.Capabilities(
            null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one");
    }
}