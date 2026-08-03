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

import com.ai.plug.core.spec.resulttype.WireSchemaValidator.Report;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WireSchemaValidatorReportJsonTest {

    @Test
    void healthyReport_minimalJson() throws Exception {
        // Use empty map (not null) to simulate a result that has been
        // processed but has no wire-layer hints at all — the validator's
        // "meta is null" path is treated as a real issue (not healthy).
        // To get a healthy report, supply a meta with a valid resultType.
        Report r = WireSchemaValidator.validateMeta(
            java.util.Map.of("resultType", "complete"), "TestSource");
        String json = r.toJson();
        assertThat(json).contains("\"source\":\"TestSource\"");
        assertThat(json).contains("\"healthy\":true");
        assertThat(json).contains("\"issueCount\":0");
        assertThat(json).doesNotContain("issues"); // empty list omitted
    }

    @Test
    void unhealthyReport_includesIssues() throws Exception {
        Report r = WireSchemaValidator.validateMeta(
            java.util.Map.of("ttlMs", -1L), "TestSource");
        String json = r.toJson();
        assertThat(json).contains("\"healthy\":false");
        assertThat(json).contains("\"issueCount\":");
        assertThat(json).contains("\"issues\":[");
        assertThat(json).contains("ttlMs must be >= 0");
    }

    @Test
    void jsonIsParseableByJackson() throws Exception {
        // Round trip — the JSON should be a valid Jackson parseable object
        Report r = WireSchemaValidator.validateMeta(
            java.util.Map.of("cacheScope", "shared"), "TestSource");
        String json = r.toJson();
        @SuppressWarnings("unchecked")
        var parsed = com.ai.plug.common.utils.JsonParser.getObjectMapper()
            .readValue(json, java.util.Map.class);
        assertThat(parsed)
            .containsEntry("source", "TestSource")
            .containsEntry("healthy", false)
            .containsKey("issueCount");
        assertThat((java.util.List<?>) parsed.get("issues")).isNotEmpty();
    }
}