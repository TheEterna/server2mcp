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

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link McpResultWriter} — verifies the wire-level JSON carries
 * the protocol 2026-07-28 fields SDK 2.0 cannot express natively.
 */
class McpResultWriterTest {

    @Test
    void writeCallToolResult_includesResultType() throws Exception {
        McpSchema.CallToolResult result = new McpSchema.CallToolResult(
            List.of(new McpSchema.TextContent("hi")), false, null, null);
        String json = McpResultWriter.writeCallToolResult(result);
        assertThat(json).contains("\"resultType\":\"complete\"");
        // Original fields preserved
        assertThat(json).contains("\"isError\"");
        assertThat(json).contains("\"content\"");
    }

    @Test
    void writeCallToolResult_withCacheHint_addsCacheable() throws Exception {
        McpSchema.CallToolResult result = new McpSchema.CallToolResult(
            List.of(new McpSchema.TextContent("cached payload")), false, null, null);
        CacheHints.Hint cache = CacheHints.of(Duration.ofSeconds(30), "public");
        String json = McpResultWriter.writeCallToolResult(result, ResultTypeConvention.COMPLETE, cache);
        assertThat(json).contains("\"resultType\":\"complete\"");
        assertThat(json).contains("\"_cacheable\"");
        assertThat(json).contains("\"ttlMs\":30000");
        assertThat(json).contains("\"cacheScope\":\"public\"");
    }

    @Test
    void writeCallToolResult_inputRequired() throws Exception {
        McpSchema.CallToolResult result = new McpSchema.CallToolResult(
            List.of(), false, null, null);
        String json = McpResultWriter.writeCallToolResult(result,
            ResultTypeConvention.INPUT_REQUIRED, null);
        assertThat(json).contains("\"resultType\":\"input_required\"");
        // No _cacheable when hint is null
        assertThat(json).doesNotContain("_cacheable");
    }

    @Test
    void writeListToolsResult_defaultsToCompleteAndAcceptsCache() throws Exception {
        McpSchema.Tool tool = McpSchema.Tool.builder().name("a").build();
        McpSchema.ListToolsResult result = new McpSchema.ListToolsResult(List.of(tool), null);
        CacheHints.Hint cache = CacheHints.of(Duration.ofMinutes(1), null); // null scope -> private
        String json = McpResultWriter.writeListToolsResult(result, cache);
        assertThat(json).contains("\"resultType\":\"complete\"");
        assertThat(json).contains("\"_cacheable\"");
        assertThat(json).contains("\"ttlMs\":60000");
        assertThat(json).contains("\"cacheScope\":\"private\"");
        assertThat(json).contains("\"name\":\"a\"");
    }

    @Test
    void writeListResourcesResult() throws Exception {
        McpSchema.Resource r = new McpSchema.Resource("r", "file:///x", "desc", "file:///x", null, null, null, null);
        McpSchema.ListResourcesResult result = new McpSchema.ListResourcesResult(List.of(r), null);
        CacheHints.Hint cache = CacheHints.of(Duration.ofSeconds(15), "public");
        String json = McpResultWriter.writeListResourcesResult(result, cache);
        assertThat(json).contains("\"resultType\":\"complete\"");
        assertThat(json).contains("\"_cacheable\"");
        assertThat(json).contains("\"ttlMs\":15000");
    }

    @Test
    void writeListPromptsResult() throws Exception {
        McpSchema.Prompt p = new McpSchema.Prompt("p", "title", "desc", null);
        McpSchema.ListPromptsResult result = new McpSchema.ListPromptsResult(List.of(p), null);
        String json = McpResultWriter.writeListPromptsResult(result, null);
        assertThat(json).contains("\"resultType\":\"complete\"");
        assertThat(json).doesNotContain("_cacheable");
    }

    @Test
    void write_customCacheWrapperKey() throws Exception {
        McpSchema.CallToolResult result = new McpSchema.CallToolResult(
            List.of(new McpSchema.TextContent("x")), false, null, null);
        CacheHints.Hint cache = CacheHints.of(Duration.ofSeconds(5), "private");
        String json = McpResultWriter.write(result, ResultTypeConvention.COMPLETE, cache, "cache");
        assertThat(json).contains("\"cache\"");
        assertThat(json).contains("\"ttlMs\":5000");
        assertThat(json).contains("\"cacheScope\":\"private\"");
        // Default wrapper key NOT emitted when custom provided
        assertThat(json).doesNotContain("\"_cacheable\"");
    }

    @Test
    void write_invalidResultTypeThrows() throws Exception {
        McpSchema.CallToolResult result = new McpSchema.CallToolResult(
            List.of(), false, null, null);
        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> McpResultWriter.writeCallToolResult(result, "bogus", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bogus");
    }

    @Test
    void write_nextCursorIsPreserved() throws Exception {
        McpSchema.Tool t = McpSchema.Tool.builder().name("a").build();
        McpSchema.ListToolsResult result = new McpSchema.ListToolsResult(List.of(t), "page-2-cursor");
        String json = McpResultWriter.writeListToolsResult(result, null);
        // nextCursor from SDK 2.0 must NOT be lost when we add resultType
        assertThat(json).contains("\"nextCursor\":\"page-2-cursor\"");
        assertThat(json).contains("\"resultType\":\"complete\"");
    }
}