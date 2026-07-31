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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WrappedCallToolResultTest {

    @Test
    void wrap_carriesBothResultAndWireJson() throws Exception {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hi").isError(false).build();
        WrappedCallToolResult wrapped = McpResultWriter.wrap(result);

        assertThat(wrapped.sdkResult()).isSameAs(result);
        assertThat(wrapped.wireJson()).contains("\"resultType\":\"complete\"");
    }

    @Test
    void wrap_withCacheHint_propagatesToWireJson() throws Exception {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("cached")
            .isError(false)
            .meta(java.util.Map.of("ttlMs", 60_000L, "cacheScope", "private"))
            .build();
        WrappedCallToolResult wrapped = McpResultWriter.wrap(result);
        assertThat(wrapped.wireJson()).contains("\"_cacheable\"")
            .contains("\"ttlMs\":60000")
            .contains("\"cacheScope\":\"private\"");
    }

    @Test
    void wrap_nullSdkResultRejected() {
        // writeCallToolResultFromMeta(null) -> NPE on .meta() access
        assertThatThrownBy(() -> McpResultWriter.wrap(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void recordConstructor_rejectsNulls() {
        assertThatThrownBy(() -> new WrappedCallToolResult(null, "{}"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WrappedCallToolResult(
            McpSchema.CallToolResult.builder().addTextContent("x").isError(false).build(), null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}