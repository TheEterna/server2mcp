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

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilitiesHealthTest {

    @Test
    void defaultListChangedAll_isHealthy() {
        var report = CapabilitiesHealth.check(
            ServerCapabilitiesFactory.withListChangedAll());
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void onlyToolsListChanged_isHealthy() {
        var report = CapabilitiesHealth.check(
            ServerCapabilitiesFactory.withToolsListChanged());
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void onlyPromptsListChanged_missingToolsFlag() {
        var report = CapabilitiesHealth.check(
            ServerCapabilitiesFactory.withPromptsListChanged());
        // Default required set includes tools.listChanged
        assertThat(report.isHealthy()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("missing required flag: tools.listChanged"));
    }

    @Test
    void customRequiredSet_acceptsEmptyRequirements() {
        // Empty set = no requirements = always healthy
        var report = CapabilitiesHealth.check(
            ServerCapabilitiesFactory.withToolsListChanged(), Set.of());
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void customRequiredSet_withTwoFlags() {
        var report = CapabilitiesHealth.check(
            ServerCapabilitiesFactory.withListChangedAll(),
            Set.of("tools.listChanged", "resources.subscribe"));
        assertThat(report.isHealthy()).isTrue();
    }

    @Test
    void customRequiredSet_oneFlagMissing() {
        var report = CapabilitiesHealth.check(
            ServerCapabilitiesFactory.withToolsListChanged(),
            Set.of("tools.listChanged", "prompts.listChanged"));
        assertThat(report.isHealthy()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("missing required flag: prompts.listChanged"));
    }

    @Test
    void customRequiredSet_oneFlagFalse() {
        // ServerCapabilities with subscribe=false: from SDK 2.0, the
        // subscribe flag is exposed — verify we detect false.
        var noSubscribe = io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.builder()
            .resources(false, true) // (subscribe=false, listChanged=true)
            .build();
        var report = CapabilitiesHealth.check(noSubscribe,
            Set.of("resources.subscribe"));
        assertThat(report.isHealthy()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("required flag is false: resources.subscribe"));
    }

    @Test
    void reportToString_healthy() {
        var report = CapabilitiesHealth.check(
            ServerCapabilitiesFactory.withListChangedAll());
        assertThat(report.toString()).contains("HEALTHY");
    }

    @Test
    void reportToString_unhealthy() {
        // Use empty caps to trigger missing required flag path
        var emptyCaps = io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.builder().build();
        var report = CapabilitiesHealth.check(emptyCaps);
        assertThat(report.isHealthy()).isFalse();
        assertThat(report.toString()).contains("UNHEALTHY");
    }
}