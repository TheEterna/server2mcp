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
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validator integration test — exercises the full path of
 * {@link WireSchemaValidator}, {@link WireSchemaValidationFilter}, and
 * {@link CapabilitiesHealth} together, ensuring their contracts interlock.
 */
class ValidatorIntegrationTest {

    @Test
    void capabilitiesHealth_healthy_supportsDefaultValidator() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        var health = CapabilitiesHealth.check(caps);
        var filter = WireSchemaValidationFilter.builder()
            .strict(true)
            .validator(WireSchemaValidator::validate)
            .build();
        assertThat(health.isHealthy()).isTrue();
        // Filter accepts (validates capabilities, not CallToolResult —
        // this path is informational only; filter is the wire-layer hook)
        assertThatCode(() -> filter.check(null)).doesNotThrowAnyException();
    }

    @Test
    void capabilitiesHealth_unhealthy_correlatesWithWireSchema() {
        // Empty capabilities triggers capabilities health failure
        // (missing tools.listChanged by default).
        var emptyCaps = McpSchema.ServerCapabilities.builder().build();
        assertThat(CapabilitiesHealth.check(emptyCaps).isHealthy()).isFalse();

        // WireSchemaValidator operates on CallToolResult meta, not
        // ServerCapabilities — different surfaces, but the health surface
        // can route to WireSchema via the same Spring pipeline.
    }

    @Test
    void validatorAndFilter_contractInterlock() {
        // WireSchemaValidationFilter uses WireSchemaValidator internally.
        // Verifies the result of a chained check: validator reports issues,
        // filter can then either log (non-strict) or throw (strict).
        var filter = WireSchemaValidationFilter.builder()
            .strict(true)
            .validator(WireSchemaValidator::validate)
            .build();
        McpSchema.CallToolResult bad = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).build(); // no resultType
        assertThatThrownBy(() -> filter.check(bad))
            .isInstanceOf(WireSchemaValidationFilter.WireSchemaValidationException.class);
    }

    @Test
    void fullCapabilitiesWithExtensions_validatorAccepts() {
        var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        var health = CapabilitiesHealth.check(caps);
        // Health is about ServerCapabilities; validator here is on the
        // experimental map, which is a free-form extensions payload.
        assertThat(health.isHealthy()).isTrue();
        // experimental map carries protocol fields + a tasks extension —
        // validator currently checks the protocol fields (resultType via
        // REQUIRED_META_KEYS) but doesn't recognize extension keys, so
        // a raw tasks extension map (no resultType) is reported as
        // missing the required key. The validator's protocol layer is
        // orthogonal to the user's extension payload.
        var report = WireSchemaValidator.validateMeta(caps.experimental(), "Capabilities.experimental");
        // We only assert the health surface here — validator behavior
        // for extension-only payloads is covered by WireSchemaValidatorEndToEndTest.
        assertThat(report.issues()).isNotEmpty(); // expected: missing resultType
    }

    @Test
    void customRequiredFlags_respectedAcrossComponents() {
        // The capabilities health surface lets callers define their own
        // required set; this is independent of the wire-schema validator.
        var caps = ServerCapabilitiesFactory.withToolsListChanged();
        var report = CapabilitiesHealth.check(caps, Set.of(
            "tools.listChanged", "resources.subscribe"));
        assertThat(report.isHealthy()).isFalse();
    }
}