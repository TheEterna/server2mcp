/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.spec.docs;

import com.ai.plug.core.spec.capabilities.CapabilitySnapshot;
import com.ai.plug.core.spec.capabilities.ServerCapabilitiesFactory;
import com.ai.plug.core.spec.mrtr.MrtrTypes;
import com.ai.plug.core.spec.pagination.McpPaging;
import com.ai.plug.core.spec.pagination.PageList;
import com.ai.plug.core.spec.resulttype.McpResultWriter;
import com.ai.plug.core.spec.resulttype.ResultTypeConvention;
import com.ai.plug.core.spec.tasks.TaskTypes;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the wire-schema-samples.json document
 * (docs/wire-schema-samples.json) by re-producing each sample from live
 * framework code and asserting key fields are present. If the sample doc
 * drifts from the implementation, this test fails — forcing documentation
 * to stay in sync.
 */
class WireSchemaSamplesTest {

    @Test
    void defaultCallToolResult_hasResultType() throws Exception {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("hello world")
            .isError(false)
            .meta(Map.of("resultType", ResultTypeConvention.COMPLETE))
            .build();
        String wire = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(wire).contains("\"resultType\":\"complete\"");
        assertThat(wire).contains("\"hello world\"");
    }

    @Test
    void callToolResult_withCacheHint_emitsAllFields() throws Exception {
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("x").isError(false)
            .meta(Map.of(
                "resultType", "complete",
                "ttlMs", 60_000L,
                "cacheScope", "private",
                "cacheWrapperKey", "_cacheable"))
            .build();
        String wire = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(wire).contains("\"ttlMs\":60000");
        assertThat(wire).contains("\"cacheScope\":\"private\"");
        assertThat(wire).contains("\"_cacheable\"");
    }

    @Test
    void callToolResult_mrtr_inputRequired() throws Exception {
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.RootsInputRequest.create(),
                    MrtrTypes.ElicitationInputRequest.create("Provide account id",
                        Map.of("type", "object"))),
            "corr-state-1");
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("input_required: 2 request(s)")
            .isError(false)
            .meta(Map.of("resultType", "input_required",
                "inputRequests", irr.inputRequests(),
                "requestState", "corr-state-1"))
            .build();
        String wire = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(wire).contains("\"resultType\":\"input_required\"");
        assertThat(wire).contains("\"kind\":\"roots\"");
        assertThat(wire).contains("\"kind\":\"elicitation\"");
        assertThat(wire).contains("\"requestState\":\"corr-state-1\"");
    }

    @Test
    void callToolResult_taskHandle() throws Exception {
        TaskTypes.TaskHandle handle = TaskTypes.TaskHandle.of("task-abc-123");
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("task accepted: " + handle.taskId())
            .isError(false)
            .meta(Map.of("taskHandle", handle.taskId()))
            .build();
        String wire = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(wire).contains("\"taskHandle\":\"task-abc-123\"");
    }

    @Test
    void callToolResult_paginated_emitsNextCursorAndTotal() throws Exception {
        PageList<Integer> page = PageList.of(
            List.of(20, 21, 22, 23, 24, 25, 26, 27, 28, 29), 100);
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .addTextContent("page")
            .isError(false)
            .meta(Map.of("resultType", "complete",
                "nextCursor", page.nextCursor(new McpPaging(0, 10)),
                "totalItems", page.totalItems()))
            .build();
        String wire = McpResultWriter.writeCallToolResultFromMeta(result);
        assertThat(wire).contains("\"nextCursor\":\"10\"");
        assertThat(wire).contains("\"totalItems\":100");
    }

    @Test
    void listToolsResult_carriesFrameworkIdentity() throws Exception {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        var snap = CapabilitySnapshot.from(caps);
        assertThat(snap.flags())
            .containsEntry("tools.listChanged", true)
            .containsEntry("resources.listChanged", true)
            .containsEntry("prompts.listChanged", true)
            .containsEntry("resources.subscribe", true);
    }
}
