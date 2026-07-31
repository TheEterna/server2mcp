/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.mrtr;

import com.ai.plug.core.spec.resulttype.McpResultWriter;
import com.ai.plug.core.spec.resulttype.ResultTypeConvention;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MrtrTypes} and the MRTR writer hook in {@link McpResultWriter}.
 * Validates wire-shape fidelity against protocol 2026-07-28 SEP-2322.
 */
class MrtrTypesTest {

    private static final ObjectMapper M = new ObjectMapper();

    @Test
    void elicitation_requestSerializesAsExpected() throws Exception {
        MrtrTypes.ElicitationInputRequest req = MrtrTypes.ElicitationInputRequest.create(
            "Please fill in your API key",
            Map.of("type", "object", "properties",
                Map.of("apiKey", Map.of("type", "string", "minLength", 8))));

        String json = M.writeValueAsString(req);
        assertThat(json).contains("\"kind\":\"elicitation\"");
        assertThat(json).contains("\"message\":\"Please fill in your API key\"");
        assertThat(json).contains("\"schema\"");
        assertThat(json).contains("\"apiKey\"");
    }

    @Test
    void elicitation_blankMessageRejected() {
        assertThatThrownBy(() -> MrtrTypes.ElicitationInputRequest.create("", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sampling_requestSerializesAsExpected() {
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "user", "content", "Hello"));
        MrtrTypes.SamplingInputRequest req = MrtrTypes.SamplingInputRequest.create(messages, 1024);
        assertThat(req.kind()).isEqualTo("sampling");
        assertThat(req.maxTokens()).isEqualTo(1024);
        assertThat(req.messages()).hasSize(1);
    }

    @Test
    void sampling_emptyMessagesRejected() {
        assertThatThrownBy(() -> MrtrTypes.SamplingInputRequest.create(List.of(), 1024))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roots_requestSerializesAsExpected() {
        MrtrTypes.RootsInputRequest req = MrtrTypes.RootsInputRequest.create();
        assertThat(req.kind()).isEqualTo("roots");
    }

    @Test
    void customKindAllowedButReservedKindsRejected() {
        // Custom kind works
        MrtrTypes.JsonSchemaInputRequest req = MrtrTypes.JsonSchemaInputRequest.create(
            "two-factor", "Enter your TOTP code", Map.of("type", "string"));
        assertThat(req.kind()).isEqualTo("two-factor");

        // Reserved kinds rejected
        assertThatThrownBy(() -> MrtrTypes.JsonSchemaInputRequest.create(
            "elicitation", "msg", Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved");
        assertThatThrownBy(() -> MrtrTypes.JsonSchemaInputRequest.create(
            "", "msg", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputRequiredResult_serialization() throws Exception {
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(
                MrtrTypes.ElicitationInputRequest.create("Pick one", Map.of("type", "object")),
                MrtrTypes.RootsInputRequest.create()
            ),
            "correlation-123");

        String json = M.writeValueAsString(irr);
        assertThat(json).contains("\"resultType\":\"input_required\"");
        assertThat(json).contains("\"inputRequests\"");
        assertThat(json).contains("\"kind\":\"elicitation\"");
        assertThat(json).contains("\"kind\":\"roots\"");
        assertThat(json).contains("\"requestState\":\"correlation-123\"");
    }

    @Test
    void inputRequiredResult_rejectsEmptyRequests() {
        assertThatThrownBy(() -> MrtrTypes.InputRequiredResult.of(List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputRequiredResult_rejectsWrongResultType() {
        assertThatThrownBy(() -> new MrtrTypes.InputRequiredResult(
            ResultTypeConvention.COMPLETE,
            List.of(MrtrTypes.RootsInputRequest.create()),
            null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("input_required");
    }

    @Test
    void inputResponses_serializesWithAnswers() throws Exception {
        MrtrTypes.InputResponses resp = MrtrTypes.InputResponses.of(
            Map.of("elicitation", Map.of("apiKey", "sk-12345678"),
                   "roots", List.of("file:///etc", "file:///var")));
        String json = M.writeValueAsString(resp);
        assertThat(json).contains("\"answers\"");
        assertThat(json).contains("\"apiKey\":\"sk-12345678\"");
        assertThat(json).contains("file:///etc");
    }

    @Test
    void inputResponses_emptyAnswersRejected() {
        assertThatThrownBy(() -> MrtrTypes.InputResponses.of(Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mcpResultWriter_writeInputRequired_emitsFullWire() throws Exception {
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.ElicitationInputRequest.create("Pick", Map.of("type", "object"))));
        String json = McpResultWriter.writeInputRequired(irr);
        assertThat(json).contains("\"resultType\":\"input_required\"");
        assertThat(json).contains("\"inputRequests\"");
    }
}