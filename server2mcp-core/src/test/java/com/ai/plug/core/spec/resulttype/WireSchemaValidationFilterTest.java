/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.resulttype;

import com.ai.plug.core.spec.resulttype.WireSchemaValidationFilter.WireSchemaValidationException;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WireSchemaValidationFilterTest {

    @Test
    void nullResultReturnsNull() {
        var filter = WireSchemaValidationFilter.builder().build();
        assertThat(filter.check(null)).isNull();
    }

    @Test
    void noValidatorConfigured_passesThrough() {
        var filter = WireSchemaValidationFilter.builder().build();
        // No validator — even invalid result passes
        McpSchema.CallToolResult bad = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).build();
        assertThat(filter.check(bad)).isSameAs(bad);
    }

    @Test
    void strictMode_validResult_passesThrough() {
        var filter = WireSchemaValidationFilter.builder()
            .strict(true)
            .validator(WireSchemaValidator::validate)
            .build();
        McpSchema.CallToolResult good = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false)
            .meta(Map.of("resultType", "complete"))
            .build();
        assertThat(filter.check(good)).isSameAs(good);
    }

    @Test
    void strictMode_invalidResult_throws() {
        var filter = WireSchemaValidationFilter.builder()
            .strict(true)
            .validator(WireSchemaValidator::validate)
            .build();
        McpSchema.CallToolResult bad = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).build(); // no resultType
        assertThatThrownBy(() -> filter.check(bad))
            .isInstanceOf(WireSchemaValidationException.class)
            .hasMessageContaining("WireSchema");
    }

    @Test
    void nonStrictMode_invalidResult_logsAndReturns() {
        var filter = WireSchemaValidationFilter.builder()
            .strict(false)
            .validator(WireSchemaValidator::validate)
            .build();
        McpSchema.CallToolResult bad = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).build();
        // Non-strict: just logs warning, returns result
        assertThat(filter.check(bad)).isSameAs(bad);
    }

    @Test
    void exception_carriesResultAndReport() {
        var filter = WireSchemaValidationFilter.builder()
            .strict(true)
            .validator(WireSchemaValidator::validate)
            .build();
        McpSchema.CallToolResult bad = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).build();
        try {
            filter.check(bad);
            throw new AssertionError("expected exception");
        }
        catch (WireSchemaValidationException ex) {
            assertThat(ex.result()).isSameAs(bad);
            assertThat(ex.report().isOk()).isFalse();
        }
    }

    @Test
    void isStrict_reflectsBuilderSetting() {
        assertThat(WireSchemaValidationFilter.builder().strict(true).build().isStrict()).isTrue();
        assertThat(WireSchemaValidationFilter.builder().strict(false).build().isStrict()).isFalse();
    }
}