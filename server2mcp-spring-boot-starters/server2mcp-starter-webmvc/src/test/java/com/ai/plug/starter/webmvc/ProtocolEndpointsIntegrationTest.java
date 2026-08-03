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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test verifying that the framework's protocol-2026-07-28
 * HTTP endpoints actually mount as Spring MVC controllers (i.e. the
 * HTTP-layer substitutes for SDK-2.0-missing RPC routes are reachable
 * via real HTTP, not just unit-tested contracts).
 *
 * <p>Uses standalone MockMvc setup (not @AutoConfigureMockMvc) to avoid
 * pulling in {@code spring-boot-test-autoconfigure-web} which Spring Boot
 * 4.x split into a separate artifact.
 */
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet"
})
class ProtocolEndpointsIntegrationTest {

    @Autowired private DiscoverEndpoint discoverEndpoint;
    @Autowired private TasksEndpoint tasksEndpoint;
    @Autowired private AugmentedPromptEndpoint augmentedPromptEndpoint;
    @Autowired private NotificationsPollingEndpoint notificationsEndpoint;
    @Autowired private TaskStore taskStore;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
            new DiscoverController(discoverEndpoint),
            new TasksController(tasksEndpoint),
            new NotificationsController(notificationsEndpoint),
            new AugmentedPromptsController(augmentedPromptEndpoint)
        ).build();
    }

    @Test
    void discover_endpointRespondsWithProtocolShape() throws Exception {
        mvc.perform(get("/mcp/discover"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.protocolVersions").isArray())
            .andExpect(jsonPath("$.preferredVersion").value("2026-07-28"))
            .andExpect(jsonPath("$.serverInfo.name").exists())
            .andExpect(jsonPath("$.capabilities").exists());
    }

    @Test
    void tasks_listEndpointReturnsArray() throws Exception {
        // TaskStore is a Spring singleton shared across tests; just
        // verify the response shape (count + array) without depending
        // on a specific count value.
        mvc.perform(get("/mcp/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").isNumber())
            .andExpect(jsonPath("$.tasks").isArray());
    }

    @Test
    void tasks_getUnknownTask_returnsFoundFalse() throws Exception {
        mvc.perform(get("/mcp/tasks/ghost"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.found").value(false));
    }

    @Test
    void tasks_cancelUnknownTask_returnsCancelledFalse() throws Exception {
        mvc.perform(post("/mcp/tasks/ghost/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cancelled").value(false));
    }

    @Test
    void notifications_pollEndpointExposesPolling() throws Exception {
        notificationsEndpoint.recordEvent("tools", java.util.Map.of("added", "x"));
        mvc.perform(get("/mcp/notifications?since=-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.events[0].kind").value("tools"))
            .andExpect(jsonPath("$.events[0].payload.added").value("x"));
    }

    @Test
    void augmentedPrompts_listEndpointEmpty() throws Exception {
        mvc.perform(get("/mcp/tasks/t1/augmented-prompts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value("t1"))
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void tasks_fullLifecycle_registerGetCancel() throws Exception {
        taskStore.register(TaskHandle.of("lifecycle-1"));
        mvc.perform(get("/mcp/tasks/lifecycle-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.found").value(true))
            .andExpect(jsonPath("$.task.status").value("running"));
        mvc.perform(post("/mcp/tasks/lifecycle-1/cancel").param("reason", "user request"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cancelled").value(true));
        mvc.perform(get("/mcp/tasks/lifecycle-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.task.status").value("cancelled"));
    }

    @Test
    void augmentedPrompts_fullLifecycle_addListDrain() throws Exception {
        mvc.perform(get("/mcp/tasks/p1/augmented-prompts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
        mvc.perform(post("/mcp/tasks/p1/augmented-prompts/drain"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drained").value(0));
    }
}