/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License.
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.capabilities;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityVersioningTest {

    @Test
    void wireVersionMap_carriesFrameworkIdentity() {
        Map<String, Object> map = CapabilityVersioning.wireVersionMap();
        assertThat(map).containsKey(CapabilityVersioning.FRAMEWORK_KEY);

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) map.get(CapabilityVersioning.FRAMEWORK_KEY);
        assertThat(info).containsKey("version");
        assertThat(info).containsKey("protocolVersions");
        assertThat(info).containsKey("wireFields");
    }

    @Test
    void wireVersionMap_protocolVersionsIncludesLatest() {
        Map<String, Object> map = CapabilityVersioning.wireVersionMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) map.get(CapabilityVersioning.FRAMEWORK_KEY);

        @SuppressWarnings("unchecked")
        List<String> versions = (List<String>) info.get("protocolVersions");
        assertThat(versions).contains("2025-11-25", "2026-07-28");
    }

    @Test
    void wireVersionMap_wireFieldsListsAllProtocolFields() {
        Map<String, Object> map = CapabilityVersioning.wireVersionMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) map.get(CapabilityVersioning.FRAMEWORK_KEY);

        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) info.get("wireFields");
        // Result-type, cache hint, MRTR, Tasks — the protocol 2026-07-28
        // fields this framework's wire layer covers
        assertThat(fields).contains("resultType", "ttlMs", "cacheScope",
            "nextCursor", "inputRequests", "requestState", "taskHandle");
    }

    @Test
    void fullCapabilities_includesListChangedAndIdentity() {
        var caps = CapabilityVersioning.fullCapabilities();
        // listChanged all (standard)
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
        assertThat(caps.prompts().listChanged()).isTrue();
        // Framework identity in experimental
        assertThat(caps.experimental()).containsKey(CapabilityVersioning.FRAMEWORK_KEY);
    }

    @Test
    void frameworkVersion_isNonEmpty() {
        assertThat(CapabilityVersioning.FRAMEWORK_VERSION).isNotBlank();
    }

    @Test
    void frameworkKey_isCorrectlyNamespaced() {
        assertThat(CapabilityVersioning.FRAMEWORK_KEY)
            .startsWith("io.modelcontextprotocol/");
    }

    @Test
    void wireVersionMap_returnsFreshMapEachTime() {
        // Defensive: caller may mutate the returned map without affecting
        // future calls.
        Map<String, Object> a = CapabilityVersioning.wireVersionMap();
        a.put("mutated", true);
        Map<String, Object> b = CapabilityVersioning.wireVersionMap();
        assertThat(b).doesNotContainKey("mutated");
    }
}