/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.headers;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link McpRequestHeaders}.
 */
class McpRequestHeadersTest {

    @Test
    void constantsHaveExpectedValues() {
        assertThat(McpRequestHeaders.MCP_METHOD).isEqualTo("Mcp-Method");
        assertThat(McpRequestHeaders.MCP_NAME).isEqualTo("Mcp-Name");
        assertThat(McpRequestHeaders.X_MCP_HEADER).isEqualTo("x-mcp-header");
    }

    @Test
    void forJsonRpcCall_includesMethodAndName() {
        Map<String, String> headers = McpRequestHeaders.forJsonRpcCall("tools/call", "my-tool");
        assertThat(headers).containsEntry("Mcp-Method", "tools/call");
        assertThat(headers).containsEntry("Mcp-Name", "my-tool");
    }

    @Test
    void forJsonRpcCall_nullNameOmitsName() {
        Map<String, String> headers = McpRequestHeaders.forJsonRpcCall("tools/list", null);
        assertThat(headers).containsOnlyKeys("Mcp-Method");
        assertThat(headers.get("Mcp-Method")).isEqualTo("tools/list");
    }

    @Test
    void forJsonRpcCall_blankNameOmitsName() {
        Map<String, String> headers = McpRequestHeaders.forJsonRpcCall("tools/list", "   ");
        assertThat(headers).doesNotContainKey("Mcp-Name");
    }

    @Test
    void forJsonRpcCall_blankMethodRejected() {
        assertThatThrownBy(() -> McpRequestHeaders.forJsonRpcCall("", "x"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> McpRequestHeaders.forJsonRpcCall(null, "x"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encodeXMcPHeader_producesKeyEqualsValue() {
        assertThat(McpRequestHeaders.encodeXMcPHeader("trace", "abc-123"))
            .isEqualTo("trace=abc-123");
    }

    @Test
    void encodeXMcPHeader_rejectsBlankKeyOrNullValue() {
        assertThatThrownBy(() -> McpRequestHeaders.encodeXMcPHeader("", "v"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> McpRequestHeaders.encodeXMcPHeader("k", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void insertOrderIsPreserved() {
        Map<String, String> headers = McpRequestHeaders.forJsonRpcCall("tools/call", "my-tool");
        // LinkedHashMap preserves insertion order; assert that the first key
        // iterated is the method header (more readable in wire captures).
        assertThat(headers.keySet().iterator().next()).isEqualTo("Mcp-Method");
    }
}