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
import com.ai.plug.core.spec.discover.DiscoverEndpoint;
import com.ai.plug.core.spec.tasks.AugmentedPromptEndpoint;
import com.ai.plug.core.spec.tasks.TaskStore;
import com.ai.plug.core.spec.tasks.TaskTypes.TaskHandle;
import com.ai.plug.core.spec.tasks.TasksEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/**
 * Integration test verifying that the framework's protocol-2026-07-28
 * HTTP endpoints actually mount as WebFlux controllers — i.e. parity
 * with the WebMVC starter. Uses {@link WebTestClient} bound to the
 * live application context (not standalone) so the auto-configuration
 * wires the controllers end-to-end.
 *
 * <p>Set {@code spring.main.web-application-type=reactive} to make
 * sure the test picks the Netty runtime even when the surrounding
 * test classpath is ambiguous.
 */
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@TestPropertySource(properties = {
    "spring.main.web-application-type=reactive"
})
class ProtocolEndpointsIntegrationTest {

    @Autowired private DiscoverEndpoint discoverEndpoint;
    @Autowired private TasksEndpoint tasksEndpoint;
    @Autowired private AugmentedPromptEndpoint augmentedPromptEndpoint;
    @Autowired private NotificationsPollingEndpoint notificationsEndpoint;
    @Autowired private TaskStore taskStore;
    @Autowired private WebTestClient webTestClient;

    @BeforeEach
    void resetState() {
        // Ensure the singleton notification buffer doesn't leak between tests.
        notificationsEndpoint.handlePoll(-1);
    }

    @Test
    void discover_endpointRespondsWithProtocolShape() {
        webTestClient.get().uri("/mcp/discover")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(body -> {
                java.util.List<?> versions = (java.util.List<?>) body.get("protocolVersions");
                if (versions == null || !versions.contains("2026-07-28")) {
                    throw new AssertionError("protocolVersions missing 2026-07-28: " + versions);
                }
                if (!"server2mcp".equals(((Map<?, ?>) body.get("serverInfo")).get("name"))) {
                    throw new AssertionError("serverInfo.name != server2mcp: " + body.get("serverInfo"));
                }
            });
    }

    @Test
    void tasks_listEndpointReturnsArray() {
        webTestClient.get().uri("/mcp/tasks")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(body -> {
                if (!(body.get("count") instanceof Number)) {
                    throw new AssertionError("count not a number: " + body.get("count"));
                }
                if (!(body.get("tasks") instanceof java.util.List)) {
                    throw new AssertionError("tasks not a list: " + body.get("tasks"));
                }
            });
    }

    @Test
    void tasks_getUnknownTask_returnsFoundFalse() {
        webTestClient.get().uri("/mcp/tasks/ghost")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(body -> {
                if (!Boolean.FALSE.equals(body.get("found"))) {
                    throw new AssertionError("expected found=false: " + body.get("found"));
                }
            });
    }

    @Test
    void tasks_cancelUnknownTask_returnsCancelledFalse() {
        webTestClient.post().uri("/mcp/tasks/ghost/cancel")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(body -> {
                if (!Boolean.FALSE.equals(body.get("cancelled"))) {
                    throw new AssertionError("expected cancelled=false: " + body.get("cancelled"));
                }
            });
    }

    @Test
    void notifications_pollEndpointExposesPolling() {
        notificationsEndpoint.recordEvent("tools", Map.of("added", "x"));
        webTestClient.get().uri("/mcp/notifications?since=-1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(body -> {
                java.util.List<?> events = (java.util.List<?>) body.get("events");
                if (events == null || events.isEmpty()) {
                    throw new AssertionError("expected at least one event: " + body);
                }
                Map<?, ?> first = (Map<?, ?>) events.get(0);
                if (!"tools".equals(first.get("kind"))) {
                    throw new AssertionError("expected kind=tools, got: " + first.get("kind"));
                }
            });
    }

    @Test
    void augmentedPrompts_listEndpointEmpty() {
        webTestClient.get().uri("/mcp/tasks/p1/augmented-prompts")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(body -> {
                if (!"p1".equals(body.get("taskId"))) {
                    throw new AssertionError("expected taskId=p1, got: " + body.get("taskId"));
                }
                if (!Integer.valueOf(0).equals(body.get("count"))) {
                    throw new AssertionError("expected count=0, got: " + body.get("count"));
                }
            });
    }

    @Test
    void tasks_fullLifecycle_registerGetCancel() {
        taskStore.register(TaskHandle.of("lifecycle-1"));

        webTestClient.get().uri("/mcp/tasks/lifecycle-1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(body -> {
                if (!Boolean.TRUE.equals(body.get("found"))) {
                    throw new AssertionError("expected found=true: " + body.get("found"));
                }
                Map<?, ?> task = (Map<?, ?>) body.get("task");
                if (!"running".equals(task.get("status"))) {
                    throw new AssertionError("expected status=running, got: " + task.get("status"));
                }
            });

        webTestClient.post().uri("/mcp/tasks/lifecycle-1/cancel")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(body -> {
                if (!Boolean.TRUE.equals(body.get("cancelled"))) {
                    throw new AssertionError("expected cancelled=true: " + body.get("cancelled"));
                }
            });

        webTestClient.get().uri("/mcp/tasks/lifecycle-1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(body -> {
                Map<?, ?> task = (Map<?, ?>) body.get("task");
                if (!"cancelled".equals(task.get("status"))) {
                    throw new AssertionError("expected status=cancelled, got: " + task.get("status"));
                }
            });
    }
}
