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

import com.ai.plug.core.spec.cacheable.CacheHints;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WireSchemaValidatorTest {

    @Test
    void validCallToolResult_ok() {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false)
            .meta(Map.of("resultType", "complete"))
            .build();
        WireSchemaValidator.Report report = WireSchemaValidator.validate(result);
        assertThat(report.isOk()).isTrue();
    }

    @Test
    void missingResultType_reported() {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false)
            .meta(Map.of()) // no resultType
            .build();
        WireSchemaValidator.Report report = WireSchemaValidator.validate(result);
        assertThat(report.isOk()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("missing required key: resultType"));
    }

    @Test
    void unknownResultType_reported() {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false)
            .meta(Map.of("resultType", "bogus"))
            .build();
        WireSchemaValidator.Report report = WireSchemaValidator.validate(result);
        assertThat(report.isOk()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("resultType has unknown value: bogus"));
    }

    @Test
    void negativeTtl_reported() {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false)
            .meta(Map.of("resultType", "complete", "ttlMs", -1L))
            .build();
        WireSchemaValidator.Report report = WireSchemaValidator.validate(result);
        assertThat(report.isOk()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("ttlMs must be >= 0"));
    }

    @Test
    void unknownCacheScope_reported() {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false)
            .meta(Map.of("resultType", "complete", "cacheScope", "shared"))
            .build();
        WireSchemaValidator.Report report = WireSchemaValidator.validate(result);
        assertThat(report.isOk()).isFalse();
        assertThat(report.issues()).anyMatch(s -> s.contains("cacheScope has unknown value: shared"));
    }

    @Test
    void validCacheScope_doesNotReport() {
        for (String scope : List.of(CacheHints.CACHE_SCOPE_PUBLIC, CacheHints.CACHE_SCOPE_PRIVATE)) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("resultType", "complete");
            meta.put("cacheScope", scope);
            McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
                .addTextContent("hi").isError(false)
                .meta(meta).build();
            WireSchemaValidator.Report report = WireSchemaValidator.validate(result);
            assertThat(report.isOk())
                .as("scope=" + scope + " should be valid")
                .isTrue();
        }
    }

    @Test
    void nullMeta_reported() {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false).meta(null).build();
        WireSchemaValidator.Report report = WireSchemaValidator.validate(result);
        assertThat(report.isOk()).isFalse();
    }

    @Test
    void validateMeta_standaloneWorks() {
        WireSchemaValidator.Report report = WireSchemaValidator.validateMeta(
            Map.of("resultType", "complete", "ttlMs", 5000L, "cacheScope", "public"),
            "TestSource");
        assertThat(report.isOk()).isTrue();
        assertThat(report.source()).isEqualTo("TestSource");
    }

    @Test
    void reportToString_ok() {
        WireSchemaValidator.Report report = new WireSchemaValidator.Report("X");
        assertThat(report.toString()).contains("X").contains("OK");
    }

    @Test
    void reportToString_withIssues() {
        WireSchemaValidator.Report report = new WireSchemaValidator.Report("Y");
        report.add("issue 1");
        report.add("issue 2");
        assertThat(report.toString()).contains("Y").contains("2 issue").contains("issue 1").contains("issue 2");
    }
}