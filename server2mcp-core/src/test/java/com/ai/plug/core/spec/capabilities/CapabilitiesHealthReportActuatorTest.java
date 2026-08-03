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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilitiesHealthReportActuatorTest {

    @Test
    void healthyCaps_reportHealthy() {
        var actuator = new CapabilitiesHealthReportActuator(
            ServerCapabilitiesFactory::withListChangedAll);
        var report = actuator.currentHealth();
        assertThat(report.healthy()).isTrue();
        assertThat(report.issueCount()).isZero();
    }

    @Test
    void unhealthyCaps_reportUnhealthy() {
        var actuator = new CapabilitiesHealthReportActuator(
            () -> McpSchema.ServerCapabilities.builder().build());
        var report = actuator.currentHealth();
        assertThat(report.healthy()).isFalse();
        assertThat(report.issueCount()).isGreaterThan(0);
    }

    @Test
    void fullCapabilitiesWithExtensions_isHealthy() {
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        var actuator = new CapabilitiesHealthReportActuator(() -> caps);
        var report = actuator.currentHealth();
        // fullCapabilitiesWithExtensions includes tools.listChanged=true
        // (default required flag), so it should be healthy.
        assertThat(report.healthy()).isTrue();
    }

    @Test
    void nullSourceReturnsHealthy() {
        var actuator = new CapabilitiesHealthReportActuator(() -> null);
        var report = actuator.currentHealth();
        assertThat(report.healthy()).isTrue();
    }

    @Test
    void sourceThrowingReturnsHealthy() {
        var actuator = new CapabilitiesHealthReportActuator(
            () -> { throw new RuntimeException("boom"); });
        var report = actuator.currentHealth();
        assertThat(report.healthy()).isTrue();
    }

    @Test
    void constructor_nullSourceRejected() {
        assertThatThrownBy(() -> new CapabilitiesHealthReportActuator(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}