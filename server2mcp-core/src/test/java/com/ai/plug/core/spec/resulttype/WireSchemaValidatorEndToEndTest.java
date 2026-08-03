/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"));
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.resulttype;

import com.ai.plug.core.spec.resulttype.WireSchemaValidator.Report;
import com.ai.plug.core.spec.cacheable.CacheHints;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of {@link WireSchemaValidator} over the full
 * protocol 2026-07-28 wire field set.
 */
class WireSchemaValidatorEndToEndTest {

    @Test
    void nullMeta_reported() {
        Report r = WireSchemaValidator.validateMeta(null, "TestSource");
        assertThat(r.isOk()).isFalse();
        assertThat(r.issues()).anyMatch(s -> s.contains("meta is null"));
    }

    @Test
    void emptyMeta_reported() {
        Report r = WireSchemaValidator.validateMeta(Map.of(), "TestSource");
        assertThat(r.isOk()).isFalse();
        assertThat(r.issues()).anyMatch(s -> s.contains("missing required key: resultType"));
    }

    @Test
    void validResultType_onlyHealthy() {
        Report r = WireSchemaValidator.validateMeta(
            Map.of("resultType", "complete"), "TestSource");
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void invalidResultTypeStringLiteral_reported() {
        Report r = WireSchemaValidator.validateMeta(
            Map.of("resultType", "not_a_real_value"), "TestSource");
        assertThat(r.isOk()).isFalse();
        assertThat(r.issues()).anyMatch(s -> s.contains("resultType has unknown value"));
    }

    @Test
    void nonStringResultType_reported() {
        Report r = WireSchemaValidator.validateMeta(
            Map.of("resultType", 42), "TestSource");
        assertThat(r.isOk()).isFalse();
        assertThat(r.issues()).anyMatch(s -> s.contains("resultType is not a String"));
    }

    @Test
    void negativeTtl_reported() {
        // resultType must be set for the default required check to pass;
        // we test only the ttlMs field in isolation by overriding the
        // required set.
        Report r = WireSchemaValidator.validateMeta(
            Map.of("resultType", "complete", "ttlMs", -100L), "TestSource",
            java.util.Set.of());  // no extra required
        assertThat(r.issues()).anyMatch(s -> s.contains("ttlMs must be >= 0"));
    }

    @Test
    void ttlMsAsStringIsParsed() {
        // ttlMs="5000" should be parsed and the report is healthy
        Report r = WireSchemaValidator.validateMeta(
            Map.of("resultType", "complete", "ttlMs", "5000"), "TestSource",
            java.util.Set.of());
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void nonNumericTtlIsIgnored() {
        Report r = WireSchemaValidator.validateMeta(
            Map.of("resultType", "complete", "ttlMs", "not-a-number"), "TestSource",
            java.util.Set.of());
        // Non-numeric ttlMs is an invalid value — validator should report it
        // (we don't silently drop, because the field is explicitly set by the
        // caller and dropping would hide a real schema violation).
        assertThat(r.isOk()).isFalse();
        assertThat(r.issues()).anyMatch(s -> s.contains("ttlMs is not a Number"));
    }

    @Test
    void unknownCacheScope_reported() {
        Report r = WireSchemaValidator.validateMeta(
            Map.of("resultType", "complete", "cacheScope", "garbage"), "TestSource",
            java.util.Set.of());
        assertThat(r.issues()).anyMatch(s -> s.contains("cacheScope has unknown value"));
    }

    @Test
    void publicCacheScope_accepted() {
        Report r = WireSchemaValidator.validateMeta(
            Map.of("resultType", "complete", "cacheScope", CacheHints.CACHE_SCOPE_PUBLIC),
            "TestSource", java.util.Set.of());
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void privateCacheScope_accepted() {
        Report r = WireSchemaValidator.validateMeta(
            Map.of("resultType", "complete", "cacheScope", CacheHints.CACHE_SCOPE_PRIVATE),
            "TestSource", java.util.Set.of());
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void fullFields_allHealthy() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("resultType", "complete");
        meta.put("ttlMs", 60_000L);
        meta.put("cacheScope", "private");
        meta.put("cacheWrapperKey", "myCache");
        Report r = WireSchemaValidator.validateMeta(meta, "TestSource");
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void fullFields_mrtr_allHealthy() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("resultType", "input_required");
        meta.put("inputRequests", java.util.List.of());
        meta.put("requestState", "corr-1");
        Report r = WireSchemaValidator.validateMeta(meta, "TestSource");
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void fullFields_taskHandle_allHealthy() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("resultType", "complete");
        meta.put("taskHandle", "task-abc-123");
        Report r = WireSchemaValidator.validateMeta(meta, "TestSource");
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void fullFields_nextCursorTotalItems_allHealthy() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("resultType", "complete");
        meta.put("nextCursor", "page-2");
        meta.put("totalItems", 100);
        Report r = WireSchemaValidator.validateMeta(meta, "TestSource");
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void fullFields_otelTrace_allHealthy() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("resultType", "complete");
        meta.put("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
        meta.put("tracestate", "rojo=00f067aa0ba902b7");
        meta.put("baggage", "userId=alice");
        Report r = WireSchemaValidator.validateMeta(meta, "TestSource");
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void fullFields_mixedMultipleIssues_reported() {
        // resultType invalid + ttlMs negative + cacheScope bogus -> 3 issues
        Map<String, Object> meta = new HashMap<>();
        meta.put("resultType", "made-up");
        meta.put("ttlMs", -1L);
        meta.put("cacheScope", "garbage");
        Report r = WireSchemaValidator.validateMeta(meta, "TestSource");
        assertThat(r.issues()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(r.isOk()).isFalse();
    }

    @Test
    void callToolResult_helper_valid() {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false)
            .meta(Map.of("resultType", "complete"))
            .build();
        Report r = WireSchemaValidator.validate(result);
        assertThat(r.isOk()).isTrue();
    }

    @Test
    void callToolResult_helper_invalid() {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false).build();
        Report r = WireSchemaValidator.validate(result);
        assertThat(r.isOk()).isFalse();
    }
}