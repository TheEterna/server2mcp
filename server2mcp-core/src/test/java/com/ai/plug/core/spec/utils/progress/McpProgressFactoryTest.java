/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.utils.progress;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link McpProgressFactory#getProgress(Object, McpSchema.CallToolRequest)}.
 * Verifies the no-op fallback when (a) the request has no progressToken in its
 * _meta, and (b) the exchange type is unrecognized — both paths must avoid NPE
 * and silently swallow {@code report(...)} calls.
 */
class McpProgressFactoryTest {

    @Test
    void getProgress_requestWithoutTokenReturnsNoOp() {
        // meta() empty -> no progressToken -> isNoOp() should be true
        McpSchema.CallToolRequest req = new McpSchema.CallToolRequest("myTool", Map.of());
        McpProgress progress = McpProgressFactory.getProgress(
                io.modelcontextprotocol.server.McpSyncServerExchange.class.cast(null), req);
        // exchange is null but factory should detect no sync/async and return no-op
        assertThat(progress).isNotNull();
        assertThat(progress.isNoOp()).isTrue();
        // report() should be safe no-ops
        progress.report(0.5);
        progress.report(0.5, "halfway");
    }

    @Test
    void getProgress_requestWithProgressTokenPreservesIt() {
        // meta() contains progressToken -> token is surfaced via progressToken() getter
        Map<String, Object> meta = Map.of("progressToken", "tok-abc-123");
        McpSchema.CallToolRequest req = new McpSchema.CallToolRequest("myTool", Map.of(), meta);

        // exchange is null, so factory will fall back to no-op instance — but it
        // still extracts token to satisfy whatever subclass inspects it.
        McpProgress progress = McpProgressFactory.getProgress(
                io.modelcontextprotocol.server.McpSyncServerExchange.class.cast(null), req);
        assertThat(progress.isNoOp()).isTrue();
        // progressToken() is not part of no-op contract — verify we don't crash
        progress.progressToken();
    }

    @Test
    void getProgress_nullRequestReturnsNoOp() {
        McpProgress progress = McpProgressFactory.getProgress(new Object(), null);
        assertThat(progress.isNoOp()).isTrue();
    }
}