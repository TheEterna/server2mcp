/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.capabilities;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityHealthEndpointTest {

    @Test
    void handle_healthyCaps_returnsHealthy() {
        var endpoint = new CapabilityHealthEndpoint(
            new CapabilitiesHealthReportActuator(
                ServerCapabilitiesFactory::withListChangedAll));
        Map<String, Object> body = endpoint.handle();
        assertThat(body).containsEntry("healthy", true);
        assertThat(body).containsEntry("issueCount", 0);
    }

    @Test
    void handle_unhealthyCaps_returnsIssues() {
        var endpoint = new CapabilityHealthEndpoint(
            new CapabilitiesHealthReportActuator(
                () -> McpSchema.ServerCapabilities.builder().build()));
        Map<String, Object> body = endpoint.handle();
        assertThat(body).containsEntry("healthy", false);
        assertThat((int) body.get("issueCount")).isGreaterThan(0);
        assertThat((java.util.List<?>) body.get("issues")).isNotEmpty();
    }

    @Test
    void handle_omitsIssuesWhenHealthy() {
        // When healthy and there are no issues, the JSON omits the issues
        // key (mirrors CapabilitiesHealthReport's @JsonInclude.NON_EMPTY).
        var endpoint = new CapabilityHealthEndpoint(
            new CapabilitiesHealthReportActuator(
                ServerCapabilitiesFactory::withListChangedAll));
        Map<String, Object> body = endpoint.handle();
        assertThat(body).doesNotContainKey("issues");
    }

    @Test
    void handle_jsonSerializable() throws Exception {
        var endpoint = new CapabilityHealthEndpoint(
            new CapabilitiesHealthReportActuator(
                () -> McpSchema.ServerCapabilities.builder().build()));
        Map<String, Object> body = endpoint.handle();
        // Round trip through Jackson — body must be a valid JSON object
        String json = com.ai.plug.common.utils.JsonParser.getObjectMapper()
            .writeValueAsString(body);
        @SuppressWarnings("unchecked")
        var parsed = com.ai.plug.common.utils.JsonParser.getObjectMapper()
            .readValue(json, Map.class);
        assertThat(parsed).containsKey("healthy");
    }

    @Test
    void constructor_nullActuatorRejected() {
        assertThatThrownBy(() -> new CapabilityHealthEndpoint(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}