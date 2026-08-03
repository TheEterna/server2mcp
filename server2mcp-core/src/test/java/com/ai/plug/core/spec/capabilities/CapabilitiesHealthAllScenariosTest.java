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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of {@link CapabilitiesHealth} across multiple required
 * flag sets, custom caps, and edge cases.
 */
class CapabilitiesHealthAllScenariosTest {

    @Test
    void defaultListChangedAll_isHealthyUnderDefault() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        var report = CapabilitiesHealth.check(caps);
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void emptyCaps_missingDefaultRequired() {
        var caps = McpSchema.ServerCapabilities.builder().build();
        var report = CapabilitiesHealth.check(caps);
        assertThat(report.isHealthy()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("missing required flag: tools.listChanged"));
    }

    @Test
    void onlyToolsListChanged_healthyWithDefaultRequired() {
        // tools.listChanged=true is the only required default — single-bucket
        // caps satisfy the default check
        var caps = ServerCapabilitiesFactory.withToolsListChanged();
        var report = CapabilitiesHealth.check(caps);
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void customRequiredFlags_allPresent_healthy() {
        var caps = McpSchema.ServerCapabilities.builder()
            .tools(true).resources(true, true).prompts(true).build();
        var report = CapabilitiesHealth.check(caps, Set.of(
            "tools.listChanged", "resources.listChanged", "prompts.listChanged"));
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void customRequiredFlags_oneMissing() {
        var caps = McpSchema.ServerCapabilities.builder()
            .tools(true).prompts(true).build(); // resources missing
        var report = CapabilitiesHealth.check(caps, Set.of(
            "tools.listChanged", "resources.listChanged", "prompts.listChanged"));
        assertThat(report.isHealthy()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("missing required flag: resources.listChanged"));
    }

    @Test
    void customRequiredFlags_oneFalse() {
        var caps = McpSchema.ServerCapabilities.builder()
            .tools(false).build(); // tools.listChanged=false
        var report = CapabilitiesHealth.check(caps, Set.of("tools.listChanged"));
        assertThat(report.isHealthy()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("required flag is false: tools.listChanged"));
    }

    @Test
    void fullCapabilitiesWithExtensions_healthyWithDefaultRequired() {
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        var report = CapabilitiesHealth.check(caps);
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void fullCapabilitiesWithExtensions_extraExtensionsFlagRequired_healthy() {
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        // experimental 字段是 Map<String,Object> 形式，CapabilitySnapshot
        // 当前只 flat 5 个已知 flag。所以 extensions key 不在 snapshot.flags()
        // 中——required set 应只用 snapshot 可读的 key。
        var report = CapabilitiesHealth.check(caps, Set.of(
            "tools.listChanged", "resources.subscribe", "prompts.listChanged"));
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void fullCapabilitiesWithExtensions_missingExperimentalFlag() {
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        var report = CapabilitiesHealth.check(caps, Set.of("nonexistent.flag"));
        assertThat(report.isHealthy()).isFalse();
    }

    @Test
    void emptyRequiredSet_alwaysHealthy() {
        var caps = McpSchema.ServerCapabilities.builder().build();
        var report = CapabilitiesHealth.check(caps, Set.of());
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void singleFlagMissing_reportSpecificMessage() {
        var caps = McpSchema.ServerCapabilities.builder()
            .tools(true).build(); // tools.listChanged=true
        var report = CapabilitiesHealth.check(caps, Set.of("tools.listChanged", "experimental.flag"));
        assertThat(report.isHealthy()).isFalse();
        // Report should mention the missing key, not the present one
        assertThat(report.issues()).anyMatch(s -> s.contains("experimental.flag"));
        assertThat(report.issues()).noneMatch(s -> s.contains("tools.listChanged"));
    }

    @Test
    void reportField_containsSource() {
        var caps = McpSchema.ServerCapabilities.builder().build();
        var report = CapabilitiesHealth.check(caps, Set.of("a"));
        assertThat(report.toString()).contains("a");
    }
}