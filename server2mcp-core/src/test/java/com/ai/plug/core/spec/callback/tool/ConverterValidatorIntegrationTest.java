/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.callback.tool;

import com.ai.plug.core.spec.resulttype.ResultTypeConvention;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterValidatorIntegrationTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty(DefaultMcpCallToolResultConverter.DEV_MODE_PROPERTY);
    }

    @Test
    void devModeOff_passesThrough() {
        // dev mode off (default) — no validation
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("ok").isError(false).build(); // no resultType!
        assertThatCode(() ->
            DefaultMcpCallToolResultConverter.maybeValidateInDevMode(result, Object.class))
            .doesNotThrowAnyException();
    }

    @Test
    void devModeOn_validMeta_passes() {
        System.setProperty(DefaultMcpCallToolResultConverter.DEV_MODE_PROPERTY, "true");
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("ok").isError(false)
            .meta(Map.of("resultType", ResultTypeConvention.COMPLETE))
            .build();
        assertThatCode(() ->
            DefaultMcpCallToolResultConverter.maybeValidateInDevMode(result, Object.class))
            .doesNotThrowAnyException();
    }

    @Test
    void devModeOn_invalidMeta_throws() {
        System.setProperty(DefaultMcpCallToolResultConverter.DEV_MODE_PROPERTY, "true");
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("ok").isError(false).build(); // no resultType!
        assertThatThrownBy(() ->
            DefaultMcpCallToolResultConverter.maybeValidateInDevMode(result, Object.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("WireSchema");
    }

    @Test
    void devModePropertyIsCaseInsensitive() {
        System.setProperty(DefaultMcpCallToolResultConverter.DEV_MODE_PROPERTY, "TRUE");
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).build();
        assertThatThrownBy(() ->
            DefaultMcpCallToolResultConverter.maybeValidateInDevMode(result, Object.class))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void devModeProperty_unrelatedValue_passes() {
        // e.g. "yes" or "1" — not "true", so validation skipped
        for (String v : new String[]{"yes", "1", "on", ""}) {
            System.setProperty(DefaultMcpCallToolResultConverter.DEV_MODE_PROPERTY, v);
            McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
                .addTextContent("x").isError(false).build();
            assertThatCode(() ->
                DefaultMcpCallToolResultConverter.maybeValidateInDevMode(result, Object.class))
                .as("value=%s should be treated as dev-mode OFF", v)
                .doesNotThrowAnyException();
        }
    }

    @Test
    void devModeProperty_constantExists() {
        // Smoke test: constant value matches the documentation
        assertThat(DefaultMcpCallToolResultConverter.DEV_MODE_PROPERTY)
            .isEqualTo("api2mcp4j.wireschema.validate");
    }

    @Test
    void returnsResult_unchanged_inDevMode() {
        System.setProperty(DefaultMcpCallToolResultConverter.DEV_MODE_PROPERTY, "true");
        Map<String, Object> meta = new HashMap<>();
        meta.put("resultType", "complete");
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("ok").isError(false)
            .meta(meta).build();
        McpSchema.CallToolResult out =
            DefaultMcpCallToolResultConverter.maybeValidateInDevMode(result, Object.class);
        assertThat(out).isSameAs(result);
    }
}