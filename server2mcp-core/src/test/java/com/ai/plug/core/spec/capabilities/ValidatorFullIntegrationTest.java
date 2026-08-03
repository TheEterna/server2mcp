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
import com.ai.plug.core.spec.resulttype.WireSchemaValidationFilter;
import com.ai.plug.core.spec.resulttype.WireSchemaValidator;
import com.ai.plug.core.spec.resulttype.WrappedCallToolResult;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full-stack integration test for the validator / filter / capabilities
 * pipeline. Exercises the wire-layer contract end-to-end.
 */
class ValidatorFullIntegrationTest {

    @Test
    void capabilitiesHealthy_callToolResultValidatedEndToEnd() throws Exception {
        // 1. Capabilities factory emits full listChanged set
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        // 2. CapabilitiesHealth passes
        assertThat(CapabilitiesHealth.check(caps).isHealthy()).isTrue();
        // 3. CapabilitiesHealthReport is healthy + zero issues
        var report = CapabilitiesHealthReport.of(CapabilitiesHealth.check(caps));
        assertThat(report.healthy()).isTrue();
        assertThat(report.issueCount()).isZero();
        // 4. CapabilityHealthEndpoint serves the report as JSON
        var endpoint = new CapabilityHealthEndpoint(
            new CapabilitiesHealthReportActuator(() -> caps));
        var body = endpoint.handle();
        assertThat(body).containsEntry("healthy", true);
        // 5. CallToolResult with cache hint passes WireSchemaValidator
        var result = McpSchema.CallToolResult.builder()
            .addTextContent("ok")
            .isError(false)
            .meta(Map.of(
                "resultType", "complete",
                "ttlMs", 60_000L,
                "cacheScope", "private",
                "cacheWrapperKey", "_cacheable"))
            .build();
        var ws = WireSchemaValidator.validate(result);
        assertThat(ws.isOk()).isTrue();
        // 6. WrappedCallToolResult packages SDK + wire
        WrappedCallToolResult wrapped = com.ai.plug.core.spec.resulttype.McpResultWriter.wrap(result);
        assertThat(wrapped.sdkResult()).isSameAs(result);
        assertThat(wrapped.wireJson()).contains("\"ttlMs\":60000");
    }

    @Test
    void capabilitiesUnhealthy_callToolResultValidatesSeparately() {
        // Capabilities unhealthy (empty caps)
        var emptyCaps = McpSchema.ServerCapabilities.builder().build();
        assertThat(CapabilitiesHealth.check(emptyCaps).isHealthy()).isFalse();
        // CallToolResult validation works independently — its input
        // is the meta map, not the capabilities.
        var result = McpSchema.CallToolResult.builder()
            .addTextContent("ok").isError(false)
            .meta(Map.of("resultType", "complete")).build();
        assertThat(WireSchemaValidator.validate(result).isOk()).isTrue();
    }

    @Test
    void strictFilter_rejectsInvalidCallToolResult() {
        var filter = WireSchemaValidationFilter.builder()
            .strict(true)
            .validator(WireSchemaValidator::validate)
            .build();
        var bad = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).build(); // no resultType
        assertThatThrownBy(() -> filter.check(bad))
            .isInstanceOf(WireSchemaValidationFilter.WireSchemaValidationException.class);
    }

    @Test
    void nonStrictFilter_passesThrough() {
        var filter = WireSchemaValidationFilter.builder()
            .strict(false)
            .validator(WireSchemaValidator::validate)
            .build();
        var bad = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).build();
        assertThatCode(() -> filter.check(bad)).doesNotThrowAnyException();
    }

    @Test
    void snapshotDiff_capturesCapabilityChanges() {
        // Before state
        var beforeCaps = ServerCapabilitiesFactory.withToolsListChanged();
        var before = CapabilitySnapshot.from(beforeCaps);
        // After state (e.g. new resources.subscribe)
        var afterCaps = ServerCapabilitiesFactory.withListChangedAll();
        var after = CapabilitySnapshot.from(afterCaps);

        // Use SnapshotCompareTool to detect the diff
        var diff = SnapshotCompareTool.compare(before, after);
        assertThat(diff.added()).extracting(SnapshotCompareTool.Change::key)
            .contains("resources.subscribe", "resources.listChanged",
                "prompts.listChanged");
        assertThat(diff.removed()).isEmpty();
        assertThat(diff.changed()).isEmpty();
    }

    @Test
    void fullCapabilitiesWithExtensions_endToEnd() throws Exception {
        // The "all fields" path
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        assertThat(CapabilitiesHealth.check(caps).isHealthy()).isTrue();
        // Health endpoint serves
        var endpoint = new CapabilityHealthEndpoint(
            new CapabilitiesHealthReportActuator(() -> caps));
        var body = endpoint.handle();
        assertThat(body).containsEntry("healthy", true);
        // Validator accepts the experimental map
        var ws = WireSchemaValidator.validateMeta(caps.experimental(),
            "Capabilities.experimental");
        // The experimental map has protocol fields (capabilities flags
        // the Snapshot doesn't include). Since experimental carries
        // 'io.modelcontextprotocol/tasks' (extension), validator with
        // default REQUIRED_META_KEYS={resultType} reports missing
        // resultType (as expected for extensions-only payload).
        assertThat(ws.issues()).anyMatch(s -> s.contains("resultType"));
    }
}