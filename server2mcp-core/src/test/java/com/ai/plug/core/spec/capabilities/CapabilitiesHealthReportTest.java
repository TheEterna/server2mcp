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
import com.ai.plug.common.utils.JsonParser;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilitiesHealthReportTest {

    @Test
    void healthy_emptyReport() throws Exception {
        var report = CapabilitiesHealthReport.of(CapabilitiesHealth.check(
            ServerCapabilitiesFactory.withListChangedAll()));
        String json = JsonParser.getObjectMapper().writeValueAsString(report);
        assertThat(json).contains("\"healthy\":true");
        assertThat(json).contains("\"issueCount\":0");
        assertThat(json).doesNotContain("issues"); // @JsonInclude(NON_EMPTY)
    }

    @Test
    void unhealthy_carriesIssues() throws Exception {
        // Use custom required flags that the default caps don't satisfy
        var report = CapabilitiesHealthReport.of(CapabilitiesHealth.check(
            ServerCapabilitiesFactory.withListChangedAll(),
            Set.of("tools.listChanged", "nonexistent.flag")));
        String json = JsonParser.getObjectMapper().writeValueAsString(report);
        assertThat(json).contains("\"healthy\":false");
        assertThat(json).contains("\"issueCount\":1");
        assertThat(json).contains("missing required flag: nonexistent.flag");
    }

    @Test
    void of_nullReport_returnsHealthy() {
        var report = CapabilitiesHealthReport.of(null);
        assertThat(report.healthy()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.issues()).isEmpty();
    }

    @Test
    void report_serializationFieldNames() throws Exception {
        var caps = McpSchema.ServerCapabilities.builder().build(); // empty
        var report = CapabilitiesHealthReport.of(CapabilitiesHealth.check(caps));
        String json = JsonParser.getObjectMapper().writeValueAsString(report);
        // Field names match the Java record
        assertThat(json).contains("\"healthy\":false");
        assertThat(json).contains("\"issueCount\":1");
        assertThat(json).contains("\"issues\":");
    }

    @Test
    void fullCapabilitiesWithExtensions_healthyAfterCheckingWithDefaultRequired() {
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        var report = CapabilitiesHealthReport.of(
            CapabilitiesHealth.check(caps));
        // With default required {tools.listChanged}, fullCapabilities should
        // be healthy (it includes tools.listChanged=true).
        assertThat(report.healthy()).isTrue();
    }
}