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

import com.ai.plug.core.spec.integration.WireSchemaExporter;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the ServerCapabilities path-combinations cover the most common
 * 8 configurations a 2026-07-28-compliant MCP server needs to advertise.
 * Each test focuses on the *intersection* of bucket flags, not the union.
 */
class ServerCapabilitiesPathCombinationsTest {

    @Test
    void scenario1_toolsOnlyListChanged() {
        var caps = ServerCapabilitiesFactory.withToolsListChanged();
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources()).isNull();
        assertThat(caps.prompts()).isNull();
    }

    @Test
    void scenario2_resourcesOnlySubscribe() {
        var caps = ServerCapabilitiesFactory.withResourcesListChanged();
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
        assertThat(caps.tools()).isNull();
        assertThat(caps.prompts()).isNull();
    }

    @Test
    void scenario3_promptsOnlyListChanged() {
        var caps = ServerCapabilitiesFactory.withPromptsListChanged();
        assertThat(caps.prompts().listChanged()).isTrue();
        assertThat(caps.tools()).isNull();
        assertThat(caps.resources()).isNull();
    }

    @Test
    void scenario4_allBucketsListChanged() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.prompts().listChanged()).isTrue();
    }

    @Test
    void scenario5_allBucketsListChanged_withTasksExtension() {
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.prompts().listChanged()).isTrue();
        assertThat(caps.experimental()).containsKey("io.modelcontextprotocol/tasks");
    }

    @Test
    void scenario6_allBuckets_listChanged_false() {
        // User explicitly turns off ALL listChanged + subscribe
        var noCaps = McpSchema.ServerCapabilities.builder().build();
        // Logger.isListChanged? — record has no field; building empty gives
        // a no-listChanged, no-subscribe, no-anything caps. We verify the
        // record fields default to null (Option 2026-07-28 = no declaration).
        assertThat(noCaps.tools()).isNull();
        assertThat(noCaps.resources()).isNull();
        assertThat(noCaps.prompts()).isNull();
        assertThat(noCaps.experimental()).isNull();
    }

    @Test
    void scenario7_capabilitiesAsMap_matchesAllBucketsShape() {
        // WireSchemaExporter.capabilitiesAsMap() 用于 spring.ai.mcp.server.capabilities.*
        // 配置文件路径——验证输出字段集与协议字段一一对应
        Map<String, Object> map = WireSchemaExporter.capabilitiesAsMap();
        assertThat(map.keySet()).containsExactlyInAnyOrder("tools", "resources", "prompts");
        assertThat((Map<String, Object>) map.get("tools")).containsEntry("listChanged", true);
        assertThat((Map<String, Object>) map.get("resources"))
            .containsEntry("subscribe", true)
            .containsEntry("listChanged", true);
        assertThat((Map<String, Object>) map.get("prompts")).containsEntry("listChanged", true);
    }

    @Test
    void scenario8_capabilitiesAsMap_isSpringAiPropertyCompatible() {
        // Each value is a plain Map<String, Object> — Spring Boot's
        // @ConfigurationProperties can deserialize into a Map<String, Map>.
        Map<String, Object> map = WireSchemaExporter.capabilitiesAsMap();
        for (var entry : map.entrySet()) {
            assertThat(entry.getValue()).isInstanceOf(Map.class);
        }
    }
}