/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.starter.webmvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the protocol-2026-07-28 JSON-RPC endpoint
 * {@code POST /mcp/jsonrpc}. Drives a full envelope through Spring MVC
 * to verify the wire shape, dispatcher routing, and error translation
 * all hang together against a real servlet stack.
 *
 * @author han
 * @time 2026/8/3
 */
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet"
})
class JsonRpcIntegrationTest {

    @Autowired private JsonRpcController jsonRpcController;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(jsonRpcController).build();
    }

    @Test
    void serverDiscover_returnsCapabilitiesEnvelope() throws Exception {
        mvc.perform(post("/mcp/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"2.0","method":"server/discover","params":{},"id":1}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jsonrpc").value("2.0"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.result.preferredVersion").value("2026-07-28"))
            .andExpect(jsonPath("$.result.serverInfo.name").exists())
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void tasksLifecycle_createGetCancel() throws Exception {
        // 1. Create
        mvc.perform(post("/mcp/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"2.0","method":"tasks/create","params":{"title":"demo"},"id":"c1"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.taskId").exists())
            .andExpect(jsonPath("$.result.status").value("running"));

        // 2. List — should contain at least one task
        mvc.perform(post("/mcp/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"2.0","method":"tasks/list","params":{},"id":"c2"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.tasks").isArray())
            .andExpect(jsonPath("$.result.count").isNumber());

        // 3. Unknown task — get returns found=false
        mvc.perform(post("/mcp/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"2.0","method":"tasks/get","params":{"taskId":"ghost"},"id":"c3"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.found").value(false));
    }

    @Test
    void unknownMethod_returnsMethodNotFoundError() throws Exception {
        mvc.perform(post("/mcp/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"2.0","method":"nonsense","params":{},"id":99}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").doesNotExist())
            .andExpect(jsonPath("$.error.code").value(-32601))
            .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("nonsense")))
            .andExpect(jsonPath("$.id").value(99));
    }

    @Test
    void invalidJsonrpc_returnsInvalidRequestError() throws Exception {
        mvc.perform(post("/mcp/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"1.0","method":"server/discover","params":{},"id":1}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error.code").value(-32600));
    }

    @Test
    void inputRequiredRespond_appendsAnswers() throws Exception {
        mvc.perform(post("/mcp/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"2.0","method":"input_required/respond",
                     "params":{"requestState":"abc-123","answers":{"street":"123 Main"}},"id":"r1"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("accepted"))
            .andExpect(jsonPath("$.result.requestState").value("abc-123"))
            .andExpect(jsonPath("$.result.answers.street").value("123 Main"));
    }
}