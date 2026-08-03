/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.test.test.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the MRTR demo controller — drives the full 3-round
 * conversation (address → payment → confirmation) via standalone MockMvc,
 * verifying the wire shape a real MCP client would see.
 *
 * @author han
 * @time 2026/8/3
 */
class MrtrDemoControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new MrtrDemoController()).build();
    }

    @Test
    void round1_returnsInputRequired_withRequestState() throws Exception {
        mvc.perform(post("/mcp-demo/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item\":\"SKU-1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("input_required"))
            .andExpect(jsonPath("$.requestState").isNotEmpty())
            .andExpect(jsonPath("$.inputRequests[0].kind").value("elicitation"));
    }

    @Test
    void threeRounds_fullFlow_completesOrder() throws Exception {
        // Round 1 — start session, capture requestState.
        MvcResult r1 = mvc.perform(post("/mcp-demo/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item\":\"SKU-1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("input_required"))
            .andReturn();
        String body1 = r1.getResponse().getContentAsString();
        String requestState = extractRequestState(body1);

        // Round 2 — supply address → expect payment prompt.
        mvc.perform(post("/mcp-demo/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item\":\"SKU-1\",\"requestState\":\"" + requestState + "\","
                    + "\"answers\":{\"street\":\"123 Main\",\"city\":\"Springfield\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("input_required"))
            .andExpect(jsonPath("$.inputRequests[0].message").value(org.hamcrest.Matchers.containsString("payment")));

        // Round 3 — supply payment → expect terminal order confirmation.
        mvc.perform(post("/mcp-demo/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item\":\"SKU-1\",\"requestState\":\"" + requestState + "\","
                    + "\"answers\":{\"method\":\"card\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("complete"))
            .andExpect(jsonPath("$.order.item").value("SKU-1"));

        // Session should be cleaned up after completion.
        mvc.perform(get("/mcp-demo/sessions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(0));
    }

    @Test
    void unknownRequestState_returnsError() throws Exception {
        mvc.perform(post("/mcp-demo/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item\":\"SKU-1\",\"requestState\":\"ghost-token\",\"answers\":{}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("error"));
    }

    /** Minimal JSON-path-free extractor for {@code requestState}. */
    private static String extractRequestState(String json) {
        int idx = json.indexOf("\"requestState\":\"");
        if (idx < 0) throw new AssertionError("requestState not found in: " + json);
        int start = idx + "\"requestState\":\"".length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}