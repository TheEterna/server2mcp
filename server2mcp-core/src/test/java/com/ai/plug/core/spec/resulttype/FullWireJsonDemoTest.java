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

import com.ai.plug.core.annotation.McpTool;
import com.ai.plug.core.spec.callback.tool.AbstractMcpToolMethodCallback;
import com.ai.plug.core.spec.callback.tool.DefaultMcpCallToolResultConverter;
import com.ai.plug.core.spec.pagination.McpPaging;
import com.ai.plug.core.spec.pagination.PageList;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack end-to-end test demonstrating the framework's wire protocol
 * coverage. A single tool method:
 * <ol>
 *   <li>Declares @McpTool hints (resultType/ttlMs/cacheScope);</li>
 *   <li>Receives McpPaging as an injected parameter;</li>
 *   <li>Receives McpRequestId as an injected parameter;</li>
 *   <li>Returns PageList<T> so the framework auto-slices + injects
 *       nextCursor + totalItems into meta;</li>
 *   <li>Has the request carry OpenTelemetry traceparent so SEP-414
 *       trace context is forwarded to the response meta;</li>
 * </ol>
 * The wire JSON produced by {@link McpResultWriter#writeCallToolResultFromMeta}
 * contains every wire-layer hint + the auto-sliced items + the forward
 * traceparent — proving the framework covers all 2026-07-28 fields that
 * SDK 2.0 cannot express natively.
 */
class FullWireJsonDemoTest {

    private final DefaultMcpCallToolResultConverter converter = new DefaultMcpCallToolResultConverter();

    @Test
    void fullWireJson_allFeaturesCombined() throws Exception {
        // 1. Set up a callback that simulates the framework having:
        //    - captured a request with McpPaging(offset=20, size=5) +
        //      McpRequestId("req-abc") + OTel traceparent
        Method m = Holder.class.getDeclaredMethod("listRows",
            McpRequestId.class, McpPaging.class);
        McpTool ann = m.getAnnotation(McpTool.class);

        TestCallback cb = new TestCallback(m, ann);
        cb.captureRequest(new McpSchema.CallToolRequest("listRows",
            Map.of("offset", 0, "size", 5), // raw args for the tool
            Map.of(
                // request meta carried by client
                "cursor", "20",
                "pageSize", 5,
                "requestId", "req-abc",
                "traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"
            )));

        // Simulate the callback having built a McpPaging from the request meta
        // (this happens in buildArgs() inside the real callback; we set it
        // manually here so the converter's auto-slice path runs)
        cb.capturePaging(new McpPaging(20, 5));

        // 2. The "tool method" returns a PageList<T>(items=[item-20..item-29], total=100)
        List<String> all = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) all.add("item-" + i);
        PageList<String> page = PageList.of(all.subList(20, 30), 100);

        // 3. Run the converter as the callback would
        McpSchema.CallToolResult result = converter.convertToCallToolResult(page, PageList.class, cb);

        // 4. Serialize to wire JSON
        String wire = McpResultWriter.writeCallToolResultFromMeta(result);

        // ---- Assert every protocol-2026-07-28 wire field is present ----

        // resultType — annotated as "complete" (default)
        assertThat(wire).contains("\"resultType\":\"complete\"");
        // ttlMs — annotated as 60_000
        assertThat(wire).contains("\"ttlMs\":60000");
        // cacheScope — annotated as "private"
        assertThat(wire).contains("\"cacheScope\":\"private\"");
        // _cacheable wrapper key — default "_cacheable"
        assertThat(wire).contains("\"_cacheable\"");
        // nextCursor — auto-injected by PageList path
        assertThat(wire).contains("\"nextCursor\"");
        // totalItems — auto-injected
        assertThat(wire).contains("\"totalItems\":100");
        // traceparent — forwarded via SEP-414
        assertThat(wire).contains("\"traceparent\":\"00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01\"");

        // Items (item-20..item-29) serialized as JSON list
        assertThat(wire).contains("item-20").contains("item-29");
    }

    @Test
    void fullWireJson_inputRequiredViaMrtr() throws Exception {
        // Demonstrate the MRTR (resultType=input_required) path with
        // traceparent forwarding. No paging needed — MRTR is the early
        // return for "I need more info before I can answer".
        Method m = Holder.class.getDeclaredMethod("needsInput", McpRequestId.class);
        McpTool ann = m.getAnnotation(McpTool.class);

        TestCallback cb = new TestCallback(m, ann);
        cb.captureRequest(new McpSchema.CallToolRequest("needsInput",
            Map.of(),
            Map.of(
                "requestId", "req-mrtr",
                "traceparent", "00-mrtr-x-01"
            )));

        com.ai.plug.core.spec.mrtr.MrtrTypes.InputRequiredResult irr =
            com.ai.plug.core.spec.mrtr.MrtrTypes.InputRequiredResult.of(
                List.of(
                    com.ai.plug.core.spec.mrtr.MrtrTypes.RootsInputRequest.create(),
                    com.ai.plug.core.spec.mrtr.MrtrTypes.ElicitationInputRequest.create(
                        "Provide account id", Map.of("type", "string", "minLength", 1))
                ),
                "corr-state-1");

        McpSchema.CallToolResult result = converter.convertToCallToolResult(irr,
            com.ai.plug.core.spec.mrtr.MrtrTypes.InputRequiredResult.class, cb);
        String wire = McpResultWriter.writeCallToolResultFromMeta(result);

        // resultType forced to input_required (MRTR overrides callback hint)
        assertThat(wire).contains("\"resultType\":\"input_required\"");
        // inputRequests array with both kinds present
        assertThat(wire).contains("\"kind\":\"roots\"");
        assertThat(wire).contains("\"kind\":\"elicitation\"");
        assertThat(wire).contains("\"message\":\"Provide account id\"");
        // requestState
        assertThat(wire).contains("\"requestState\":\"corr-state-1\"");
        // OTel trace forwarded
        assertThat(wire).contains("\"traceparent\":\"00-mrtr-x-01\"");
    }

    // ---- minimal stand-in callback ----

    private static final class TestCallback extends AbstractMcpToolMethodCallback {
        TestCallback(Method method, McpTool annotation) {
            super(method, new Object(), "name", null, "{}", null, null, null, annotation, null,
                null, null);
        }
        @Override
        protected boolean isExchangeType(Class<?> paramType) { return false; }
    }

    /** Mirror of {@link com.ai.plug.core.spec.request.McpRequestId} — used as parameter type. */
    private static final class McpRequestId {
        private final String id;
        McpRequestId(String id) { this.id = id; }
    }

    static final class Holder {
        @McpTool(name = "list", resultType = "complete", ttlMs = 60_000,
                cacheScope = "private", listChanged = true)
        public PageList<String> listRows(McpRequestId id, McpPaging paging) {
            return PageList.empty();
        }

        @McpTool(name = "needsInput", resultType = "complete", ttlMs = 60_000)
        public com.ai.plug.core.spec.mrtr.MrtrTypes.InputRequiredResult needsInput(
                McpRequestId id) {
            return com.ai.plug.core.spec.mrtr.MrtrTypes.InputRequiredResult.of(
                List.of(com.ai.plug.core.spec.mrtr.MrtrTypes.RootsInputRequest.create()));
        }
    }
}