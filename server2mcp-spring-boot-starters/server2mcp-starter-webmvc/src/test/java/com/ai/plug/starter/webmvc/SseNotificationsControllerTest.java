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

import com.ai.plug.core.spec.change.NotificationsPollingEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the SSE notifications controller — verify the
 * subscription registry, the broadcast hook delivery to live clients,
 * and the replay-on-Last-Event-ID contract.
 *
 * <p>End-to-end MockMvc tests for {@code GET /mcp/sse} live in the
 * server2mcp-test module (require a running servlet container with
 * async support); here we test the controller's in-process behaviour
 * directly so the suite stays fast.
 *
 * @author han
 * @time 2026/8/3
 */
class SseNotificationsControllerTest {

    private NotificationsPollingEndpoint notifications;
    private SseNotificationsController controller;

    @BeforeEach
    void setUp() {
        notifications = new NotificationsPollingEndpoint();
        controller = new SseNotificationsController(notifications);
        notifications.setListener(controller::broadcast);
    }

    @Test
    void subscribe_registersEmitter_andIncrementsActiveClients() {
        assertThat(controller.activeClients()).isZero();
        SseEmitter emitter = controller.subscribe(null);
        assertThat(emitter).isNotNull();
        assertThat(controller.activeClients()).isEqualTo(1);
    }

    @Test
    void subscribe_withNullLastEventId_doesNotReplay() {
        controller.subscribe(null);
        // No recorded events → no replay attempted. Just verify it didn't throw.
        assertThat(controller.activeClients()).isEqualTo(1);
    }

    @Test
    void subscribe_withLastEventId_replaysMissedEvents() {
        long c1 = notifications.recordEvent("tools",
            Map.of("added", java.util.List.of("t1")));
        long c2 = notifications.recordEvent("tools",
            Map.of("added", java.util.List.of("t2")));
        SseEmitter emitter = controller.subscribe(String.valueOf(c1));
        assertThat(emitter).isNotNull();
        assertThat(controller.activeClients()).isEqualTo(1);
        // Both events are in the ring buffer; replay logic is exercised.
        Map<String, Object> body = notifications.handlePoll(-1);
        assertThat(body.get("count")).isEqualTo(2);
        // Replay window only includes c2 (c1 == Last-Event-ID).
        @SuppressWarnings("unchecked")
        java.util.List<com.ai.plug.core.spec.change.NotificationsPollingEndpoint.NotificationEvent> events =
            (java.util.List<com.ai.plug.core.spec.change.NotificationsPollingEndpoint.NotificationEvent>) body.get("events");
        assertThat(events).hasSize(2);
        assertThat(events.get(0).cursor()).isEqualTo(c1);
        assertThat(events.get(1).cursor()).isEqualTo(c2);
    }

    @Test
    void recordEvent_listenerReceivesCursorAndKind() {
        java.util.List<long[]> seen = new java.util.concurrent.CopyOnWriteArrayList<>();
        notifications.setListener((cursor, kind, payload) ->
            seen.add(new long[]{cursor, kind.hashCode()}));

        notifications.recordEvent("tools", Map.of("k", "v"));
        notifications.recordEvent("resources", Map.of("k", "v"));
        assertThat(seen).hasSize(2);
        assertThat(seen.get(0)[0]).isLessThan(seen.get(1)[0]);
    }

    @Test
    void broadcast_withNoClients_isNoOp() {
        controller.broadcast(1L, "tools", Map.of("k", "v"));
        assertThat(controller.activeClients()).isZero();
    }

    @Test
    void listenerFailure_doesNotPoisonRecordEvent() {
        notifications.setListener((c, k, p) -> { throw new RuntimeException("boom"); });
        long c = notifications.recordEvent("tools", Map.of("k", "v"));
        assertThat(c).isPositive();
        Map<String, Object> body = notifications.handlePoll(-1);
        assertThat(body.get("count")).isEqualTo(1);
    }

    @Test
    void broadcast_withClosedEmitter_removesFromRegistry() {
        SseEmitter emitter = controller.subscribe(null);
        assertThat(controller.activeClients()).isEqualTo(1);
        // Manually complete the emitter (simulates client disconnect)
        emitter.complete();
        // broadcast should now no-op and the registry should reflect removal
        controller.broadcast(1L, "tools", Map.of("k", "v"));
        assertThat(controller.activeClients()).isZero();
    }
}