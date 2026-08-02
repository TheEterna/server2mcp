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

import com.ai.plug.core.spec.capabilities.ServerCapabilitiesFactory;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WireSchemaValidationListenerTest {

    @Test
    void validCaps_loggedOk() {
        AtomicReference<String> captured = new AtomicReference<>();
        WireSchemaValidationListener listener = new WireSchemaValidationListener(
            ServerCapabilitiesFactory::withListChangedAll);
        // Capture the call result via direct method invocation
        listener.onApplicationReady();
        // No throw = OK (logger writes to SLF4J; we just verify no exception)
    }

    @Test
    void invalidCaps_defaultListenerLogsAndContinues() {
        // Empty caps: missing resultType in meta (we use the standalone
        // schema validation path; meta is null which counts as "no wire hints")
        var emptyCaps = McpSchema.ServerCapabilities.builder().build();
        WireSchemaValidationListener listener = new WireSchemaValidationListener(
            () -> emptyCaps);
        // Non-strict: just logs (no throw)
        listener.onApplicationReady();
    }

    @Test
    void invalidCaps_strictListenerThrows() {
        var emptyCaps = McpSchema.ServerCapabilities.builder().build();
        WireSchemaValidationListener listener = new WireSchemaValidationListener(
            () -> emptyCaps, true);
        // Strict: must throw IllegalStateException
        assertThatThrownBy(listener::onApplicationReady)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("WireSchema");
    }

    @Test
    void source_throws_isSwallowed() {
        WireSchemaValidationListener listener = new WireSchemaValidationListener(
            () -> { throw new RuntimeException("boom"); });
        // Non-strict + source throws: just log warning
        listener.onApplicationReady();
    }

    @Test
    void constructor_nullSourceRejected() {
        assertThatThrownBy(() -> new WireSchemaValidationListener(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}