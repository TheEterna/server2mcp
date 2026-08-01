/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"));
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.integration;

import com.ai.plug.core.spec.capabilities.ServerCapabilitiesFactory;
import com.ai.plug.core.spec.implementation.ServerInfoFactory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerAnnounceEventBridgeTest {

    @Test
    void emit_invokesSinkWithAnnounce() {
        AtomicReference<ServerAnnounce> captured = new AtomicReference<>();
        ServerAnnounce ann = ServerAnnounce.minimal("svc", "1.0");
        ServerAnnounceEventBridge bridge = new ServerAnnounceEventBridge(ann, captured::set);

        bridge.emit();

        assertThat(captured.get()).isSameAs(ann);
    }

    @Test
    void onApplicationReady_invokesSink() {
        AtomicInteger callCount = new AtomicInteger();
        ServerAnnounceEventBridge bridge = new ServerAnnounceEventBridge(
            ServerAnnounce.minimal("svc", "1.0"), a -> callCount.incrementAndGet());

        bridge.onApplicationReady();

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void emit_withNullSink_isNoOp() {
        ServerAnnounceEventBridge bridge = new ServerAnnounceEventBridge(
            ServerAnnounce.minimal("svc", "1.0"), null);
        // Should not throw
        bridge.emit();
    }

    @Test
    void emit_sinkExceptionSwallowed() {
        ServerAnnounceEventBridge bridge = new ServerAnnounceEventBridge(
            ServerAnnounce.builder()
                .info(ServerInfoFactory.create("svc", "1.0"))
                .capabilities(ServerCapabilitiesFactory.withToolsListChanged())
                .build(),
            a -> { throw new RuntimeException("boom"); });
        // Should not throw
        bridge.emit();
    }

    @Test
    void announce_returnsConfiguredAnnounce() {
        ServerAnnounce ann = ServerAnnounce.minimal("svc", "1.0");
        ServerAnnounceEventBridge bridge = new ServerAnnounceEventBridge(ann, null);
        assertThat(bridge.announce()).isSameAs(ann);
    }

    @Test
    void constructor_nullAnnounceRejected() {
        assertThatThrownBy(() -> new ServerAnnounceEventBridge(null, a -> {}))
            .isInstanceOf(IllegalArgumentException.class);
    }
}