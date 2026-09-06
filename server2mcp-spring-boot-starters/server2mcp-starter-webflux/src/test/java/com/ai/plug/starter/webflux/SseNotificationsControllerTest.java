/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.starter.webflux;

import com.ai.plug.core.spec.change.NotificationsPollingEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

/**
 * Verifies the WebFlux SSE controller wires correctly with the
 * framework's {@link NotificationsPollingEndpoint}:
 * <ul>
 *   <li>the stream is text/event-stream;</li>
 *   <li>the initial {@code connected} comment arrives before any
 *       events;</li>
 *   <li>{@code recordEvent} on the polling endpoint is broadcast to
 *       live SSE clients.</li>
 * </ul>
 *
 * <p>Uses {@link WebTestClient#get().exchange().returnResult(Class)} to
 * capture the raw SSE {@link Flux} so we can run {@link StepVerifier}
 * against it without holding the HTTP connection open past the test
 * boundary.
 */
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@TestPropertySource(properties = {
    "spring.main.web-application-type=reactive"
})
class SseNotificationsControllerTest {

    @Autowired private NotificationsPollingEndpoint notificationsEndpoint;
    @Autowired private WebTestClient webTestClient;

    @BeforeEach
    void resetState() {
        // Drain any leftover events from previous tests.
        notificationsEndpoint.handlePoll(-1);
    }

    @Test
    void subscribe_emitsInitialConnectedComment() {
        FluxExchangeResult<String> result = webTestClient.get()
            .uri("/mcp/sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class);

        StepVerifier.create(result.getResponseBody()
                .filter(line -> line != null && !line.isEmpty())
                .take(Duration.ofMillis(300))
                .collectList())
            .assertNext(lines -> {
                // First non-empty line should be the connected comment.
                if (lines.isEmpty() || !lines.get(0).startsWith(":")) {
                    throw new AssertionError("expected first line to be an SSE comment, got: " + lines);
                }
            })
            .verifyComplete();
    }

    @Test
    void broadcast_pushesRecordedEventsToLiveClients() throws Exception {
        // Subscribe in a background thread, then record an event, then verify the
        // SSE client observed it.
        FluxExchangeResult<String> result = webTestClient.get()
            .uri("/mcp/sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class);

        // Give the subscription a moment to wire up.
        Thread.sleep(200);

        // Trigger a recorded event from the polling endpoint — the
        // auto-configuration's listener hook will broadcast to SSE.
        notificationsEndpoint.recordEvent("tools", Map.of("added", "reactive-test-tool"));

        StepVerifier.create(result.getResponseBody()
                .filter(line -> line != null && line.startsWith("data:"))
                .map(line -> line.substring("data:".length()).trim())
                .take(Duration.ofSeconds(2))
                .collectList())
            .assertNext(payloads -> {
                if (payloads.isEmpty()) {
                    throw new AssertionError("expected at least one SSE data frame within 2s, got none");
                }
            })
            .verifyComplete();
    }
}
