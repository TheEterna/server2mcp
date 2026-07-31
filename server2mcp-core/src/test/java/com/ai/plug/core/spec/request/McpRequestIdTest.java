/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpRequestIdTest {

    @Test
    void of_nullIsNoneSingleton() {
        assertThat(McpRequestId.of(null)).isSameAs(McpRequestId.NONE);
        assertThat(McpRequestId.NONE.isPresent()).isFalse();
    }

    @Test
    void of_withValueIsPresent() {
        McpRequestId id = McpRequestId.of("abc-123");
        assertThat(id.id()).isEqualTo("abc-123");
        assertThat(id.isPresent()).isTrue();
    }

    @Test
    void synthetic_producesUniquePrefixedId() {
        McpRequestId a = McpRequestId.synthetic("call");
        McpRequestId b = McpRequestId.synthetic("call");
        assertThat(a.id()).startsWith("call-");
        assertThat(b.id()).startsWith("call-");
        assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void synthetic_nullPrefixDefaultsToReq() {
        McpRequestId id = McpRequestId.synthetic(null);
        assertThat(id.id()).startsWith("req-");
    }
}