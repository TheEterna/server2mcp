/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.integration;

import com.ai.plug.core.spec.capabilities.ServerCapabilitiesFactory;
import com.ai.plug.core.spec.implementation.ServerInfoFactory;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link McpServerCustomizers}'s customizers invoke the
 * correct SDK methods on a {@code McpServer.SyncSpecification} (the
 * pre-build configuration object that Spring AI 2.0 uses to assemble an
 * MCP server).
 *
 * <p>We can't easily construct a real
 * {@code StreamableSyncSpecification} (its private ctor requires a non-null
 * transport), so we use a minimal subclass of the abstract
 * {@code SyncSpecification} that overrides the protected setters. The
 * customizer's job is to call those setters; verifying that the setters
 * received the expected values is sufficient end-to-end coverage.
 */
class McpServerCustomizersEndToEndTest {

    @Test
    void syncListChangedAll_callsCapabilitiesSetter() {
        var holder = new SpecHolder();
        McpServerCustomizers.syncListChangedAll().customize(holder.spec);
        assertThat(holder.caps.get())
            .as("customizer should have invoked spec.capabilities(...)")
            .isNotNull();
        var caps = holder.caps.get();
        assertThat(caps.tools().listChanged()).isTrue();
        assertThat(caps.resources().subscribe()).isTrue();
        assertThat(caps.resources().listChanged()).isTrue();
        assertThat(caps.prompts().listChanged()).isTrue();
    }

    @Test
    void serverInfo_callsServerInfoSetter() {
        var holder = new SpecHolder();
        McpServerCustomizers.serverInfo("my-mcp", "2.0", "My MCP", "Hello world")
            .customize(holder.spec);
        assertThat(holder.info.get())
            .as("customizer should have invoked spec.serverInfo(...)")
            .isNotNull();
        var info = holder.info.get();
        assertThat(info.name()).isEqualTo("my-mcp");
        assertThat(info.version()).isEqualTo("2.0");
        assertThat(info.title()).isEqualTo("My MCP");
        assertThat(info.description()).isEqualTo("Hello world");
    }

    @Test
    void composeAll_appliesBothCustomizers() {
        var holder = new SpecHolder();
        McpServerCustomizers.composeAll(
            McpServerCustomizers.syncListChangedAll(),
            McpServerCustomizers.serverInfo("svc", "1.0")
        ).customize(holder.spec);
        assertThat(holder.caps.get().tools().listChanged()).isTrue();
        assertThat(holder.info.get().name()).isEqualTo("svc");
    }

    @Test
    void serverInfoFactory_doesNotCrash() {
        var impl = ServerInfoFactory.create("svc", "1.0", "Title", "Desc");
        assertThat(impl).isNotNull();
        assertThat(impl.name()).isEqualTo("svc");
        assertThat(impl.title()).isEqualTo("Title");
    }

    @Test
    void capabilitiesFactory_doesNotCrash() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        assertThat(caps).isNotNull();
        assertThat(caps.tools().listChanged()).isTrue();
    }

    /**
     * Minimal {@code SyncSpecification} subclass that captures the values the
     * customizer would set via fluent {@code spec.capabilities(...)} /
     * {@code spec.serverInfo(...)} calls. We override those specific methods
     * (which are declared on the parent spec class) to record the values
     * in {@link AtomicReference} fields so the test can read them back.
     */
    private static final class SpecHolder {
        final AtomicReference<McpSchema.ServerCapabilities> caps = new AtomicReference<>();
        final AtomicReference<McpSchema.Implementation> info = new AtomicReference<>();
        final McpServer.SyncSpecification<?> spec = new NoopSyncSpec() {
            @Override
            public NoopSyncSpec capabilities(McpSchema.ServerCapabilities capabilities) {
                caps.set(capabilities);
                return this;
            }
            @Override
            public NoopSyncSpec serverInfo(String name, String version) {
                info.set(new McpSchema.Implementation(name, version));
                return this;
            }
            @Override
            public NoopSyncSpec serverInfo(McpSchema.Implementation implementation) {
                info.set(implementation);
                return this;
            }
        };
    }

    /** Trivial no-op SyncSpecification for type capture. */
    private static class NoopSyncSpec
            extends McpServer.SyncSpecification<NoopSyncSpec> {
        @Override
        public io.modelcontextprotocol.server.McpSyncServer build() {
            throw new UnsupportedOperationException();
        }
    }
}