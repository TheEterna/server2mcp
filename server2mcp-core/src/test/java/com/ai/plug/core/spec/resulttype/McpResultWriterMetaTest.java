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

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link McpResultWriter#writeCallToolResultFromMeta(McpSchema.CallToolResult)}.
 * Verifies that the meta fields written by {@code DefaultMcpCallToolResultConverter}
 * (via @McpTool hints) are correctly read back and applied to the wire payload.
 */
class McpResultWriterMetaTest {

    @Test
    void fromMeta_extractsCompleteByDefault() throws Exception {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false).build();
        // meta empty -> default resultType=complete, no _cacheable
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(json).contains("\"resultType\":\"complete\"");
        assertThat(json).doesNotContain("_cacheable");
    }

    @Test
    void fromMeta_extractsInputRequiredOverride() throws Exception {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("input_required: 1 request(s)").isError(false)
            .meta(Map.of("resultType", "input_required")).build();
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(json).contains("\"resultType\":\"input_required\"");
    }

    @Test
    void fromMeta_extractsCacheHint() throws Exception {
        Map<String, Object> meta = new HashMap<>();
        meta.put("resultType", "complete");
        meta.put("ttlMs", 60_000L);
        meta.put("cacheScope", "private");
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("cached").isError(false).meta(meta).build();
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(json).contains("\"resultType\":\"complete\"");
        assertThat(json).contains("\"_cacheable\"");
        assertThat(json).contains("\"ttlMs\":60000");
        assertThat(json).contains("\"cacheScope\":\"private\"");
    }

    @Test
    void fromMeta_omitsCacheHintWhenTtlZero() throws Exception {
        Map<String, Object> meta = new HashMap<>();
        meta.put("ttlMs", 0L);
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("no cache").isError(false).meta(meta).build();
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(json).doesNotContain("_cacheable");
    }

    @Test
    void fromMeta_nullMetaHandled() throws Exception {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).meta(null).build();
        // Should not throw — null meta path falls back to defaults
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(json).contains("\"resultType\":\"complete\"");
    }

    @Test
    void fromMeta_invalidResultTypeStringFallsBack() throws Exception {
        // Defensive: if meta carries a non-spec resultType, fall back to complete
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).meta(Map.of("resultType", "bogus")).build();
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        // Validate guard rejected the bad value and we fell back to complete
        assertThat(json).contains("\"resultType\":\"complete\"");
    }

    @Test
    void fromMeta_invalidCacheScopeFallsBackToPrivate() throws Exception {
        // Bad scope value gets normalized to private (CacheHints.normalizeScope fallback)
        Map<String, Object> meta = new HashMap<>();
        meta.put("ttlMs", 1000L);
        meta.put("cacheScope", "shared");
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false).meta(meta).build();
        String json = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(json).contains("\"cacheScope\":\"private\"");
    }

    @Test
    void listToolsResultFromMeta_returnsCompleteByDefault() throws Exception {
        McpSchema.Tool t = McpSchema.Tool.builder().name("a").build();
        McpSchema.ListToolsResult result = new McpSchema.ListToolsResult(List.of(t), null);
        String json = McpResultWriter.writeListToolsResultFromMeta(result);
        assertThat(json).contains("\"resultType\":\"complete\"");
    }

    @Test
    void cacheHintFromMeta_extractsTtlAndScope() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("ttlMs", 30_000L);
        meta.put("cacheScope", "public");
        var hint = McpResultWriter.cacheHintFromMeta(meta);
        assertThat(hint).isNotNull();
        assertThat(hint.ttlMs()).isEqualTo(30_000L);
        assertThat(hint.cacheScope()).isEqualTo("public");
    }

    @Test
    void cacheHintFromMeta_zeroTtlReturnsNull() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("ttlMs", 0L);
        assertThat(McpResultWriter.cacheHintFromMeta(meta)).isNull();
    }

    @Test
    void cacheHintFromMeta_nullMetaReturnsNull() {
        assertThat(McpResultWriter.cacheHintFromMeta(null)).isNull();
    }
}