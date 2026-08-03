/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.change;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationsPollingEndpointTest {

    @Test
    void emptyEndpoint_returnsZeroEvents() {
        var ep = new NotificationsPollingEndpoint();
        Map<String, Object> body = ep.handlePoll(-1);
        assertThat(body).containsEntry("count", 0);
        assertThat((List<?>) body.get("events")).isEmpty();
    }

    @Test
    void recordEvent_returnsMonotonicCursor() {
        var ep = new NotificationsPollingEndpoint();
        long c1 = ep.recordEvent("tools", Map.of("added", "foo"));
        long c2 = ep.recordEvent("resources", Map.of("removed", "bar"));
        assertThat(c2).isGreaterThan(c1);
        assertThat(ep.currentCursor()).isEqualTo(c2);
    }

    @Test
    void handlePoll_returnsEventsAfterCursor() {
        var ep = new NotificationsPollingEndpoint();
        long c1 = ep.recordEvent("tools", Map.of("k", 1));
        long c2 = ep.recordEvent("tools", Map.of("k", 2));
        long c3 = ep.recordEvent("tools", Map.of("k", 3));
        // Poll since c1 → should see c2 + c3
        Map<String, Object> body = ep.handlePoll(c1);
        assertThat(body).containsEntry("count", 2);
        assertThat(body).containsEntry("nextCursor", c3);
        @SuppressWarnings("unchecked")
        var events = (List<NotificationsPollingEndpoint.NotificationEvent>) body.get("events");
        assertThat(events).hasSize(2);
        assertThat(events.get(0).cursor()).isEqualTo(c2);
        assertThat(events.get(1).cursor()).isEqualTo(c3);
    }

    @Test
    void handlePoll_minusOne_returnsAll() {
        var ep = new NotificationsPollingEndpoint();
        ep.recordEvent("tools", Map.of());
        ep.recordEvent("resources", Map.of());
        Map<String, Object> body = ep.handlePoll(-1);
        assertThat(body).containsEntry("count", 2);
    }

    @Test
    void ringBuffer_evictsOldest_whenCapacityExceeded() {
        var ep = new NotificationsPollingEndpoint(16);
        // Record 30 events; only the last 16 should remain
        for (int i = 0; i < 30; i++) {
            ep.recordEvent("tools", Map.of("i", i));
        }
        assertThat(ep.bufferedCount()).isEqualTo(16);
        // Cursor must still reflect all 30 events issued
        assertThat(ep.currentCursor()).isEqualTo(30);
    }

    @Test
    void recordEvent_validatesKind() {
        var ep = new NotificationsPollingEndpoint();
        assertThatThrownBy(() -> ep.recordEvent(null, Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ep.recordEvent("", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_validatesCapacity() {
        assertThatThrownBy(() -> new NotificationsPollingEndpoint(8))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clear_resetsState() {
        var ep = new NotificationsPollingEndpoint();
        ep.recordEvent("tools", Map.of());
        ep.recordEvent("tools", Map.of());
        ep.clear();
        assertThat(ep.bufferedCount()).isZero();
        assertThat(ep.currentCursor()).isZero();
    }

    @Test
    void handlePollJson_serializes() throws Exception {
        var ep = new NotificationsPollingEndpoint();
        ep.recordEvent("tools", Map.of("added", "x"));
        String json = ep.handlePollJson(-1);
        assertThat(json).contains("\"nextCursor\":1");
        assertThat(json).contains("\"count\":1");
        assertThat(json).contains("\"cursor\":1");
        assertThat(json).contains("\"kind\":\"tools\"");
        assertThat(json).contains("\"payload\"");
        assertThat(json).contains("\"timestamp\"");
    }

    @Test
    void notificationEvent_immutablePayload() {
        var ep = new NotificationsPollingEndpoint();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("k", "v");
        ep.recordEvent("tools", payload);
        Map<String, Object> body = ep.handlePoll(-1);
        @SuppressWarnings("unchecked")
        var events = (List<NotificationsPollingEndpoint.NotificationEvent>) body.get("events");
        assertThat(events).hasSize(1);
        // payload is defensively copied (Map.copyOf) — mutating the
        // original doesn't change the recorded event
        payload.put("k", "MUTATED");
        assertThat(events.get(0).payload()).containsEntry("k", "v");
    }
}