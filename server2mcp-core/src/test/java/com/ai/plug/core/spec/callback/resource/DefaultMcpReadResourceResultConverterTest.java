/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.callback.resource;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultMcpReadResourceResultConverter} — the framework's
 * resource callback produces McpSchema.ReadResourceResult which the protocol
 * can serve as-is (resource reads don't need the new 2026-07-28 wire fields).
 */
class DefaultMcpReadResourceResultConverterTest {

    private final DefaultMcpReadResourceResultConverter converter =
        new DefaultMcpReadResourceResultConverter();

    @Test
    void stringResultProducesTextResourceContents() {
        McpSchema.ReadResourceResult result = converter.convertToReadResourceResult(
            "file content", "file:///tmp/x.txt", "text/plain", null);
        assertThat(result).isNotNull();
        assertThat(result.contents()).hasSize(1);
        assertThat(result.contents().get(0)).isInstanceOf(McpSchema.TextResourceContents.class);
    }

    @Test
    void byteArrayResult_throwsForUnsupportedType() {
        // DefaultMcpReadResourceResultConverter doesn't unwrap byte[] — it
        // expects user pre-built ResourceContents. Verify the documented
        // exception path.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            converter.convertToReadResourceResult(
                new byte[]{1, 2, 3}, "file:///tmp/x.bin", "application/octet-stream", null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Unsupported return type");
    }

    @Test
    void preBuiltResultPassesThrough() {
        McpSchema.TextResourceContents contents =
            new McpSchema.TextResourceContents("file:///x.txt", "hi", "text/plain");
        McpSchema.ReadResourceResult input = new McpSchema.ReadResourceResult(
            java.util.List.of(contents));
        McpSchema.ReadResourceResult output = converter.convertToReadResourceResult(
            input, "file:///x.txt", "text/plain", null);
        assertThat(output).isSameAs(input);
    }
}