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
import com.ai.plug.core.spec.jsonrpc.JsonRpcRouter;
import com.ai.plug.core.spec.jsonrpc.JsonRpcRoutes;
import com.ai.plug.core.spec.mrtr.InMemoryMrtrSessionStore;
import com.ai.plug.core.spec.mrtr.MrtrSessionStore;
import com.ai.plug.core.spec.tasks.AugmentedPromptEndpoint;
import com.ai.plug.core.spec.tasks.AugmentedPromptStore;
import com.ai.plug.core.spec.tasks.InMemoryAugmentedPromptStore;
import com.ai.plug.core.spec.tasks.InMemoryTaskStore;
import com.ai.plug.core.spec.tasks.TaskStore;
import com.ai.plug.core.spec.tasks.TasksEndpoint;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Auto-configuration that mounts the framework's protocol-2026-07-28
 * HTTP contracts on the <b>reactive</b> Spring stack. The end-user API
 * is identical to {@code server2mcp-starter-webmvc}; only the
 * underlying runtime is Netty instead of Tomcat.
 *
 * <p>Endpoints exposed:
 * <ul>
 *   <li>{@code GET /mcp/discover} → {@link DiscoverController}</li>
 *   <li>{@code POST /mcp/jsonrpc} → {@link JsonRpcController}</li>
 *   <li>{@code /mcp/tasks}* → {@link TasksController}</li>
 *   <li>{@code /mcp/tasks/{id}/augmented-prompts}* → {@link AugmentedPromptsController}</li>
 *   <li>{@code GET /mcp/notifications} → {@link NotificationsController}</li>
 *   <li>{@code GET /mcp/sse} → {@link SseNotificationsController}</li>
 * </ul>
 *
 * <p>When Java MCP SDK ≥ 3.0.0 ships its native reactive routes, the
 * controllers here remain as a fallback for older clients — no
 * business-code change required.
 *
 * @author han
 * @time 2026/8/3
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ProtocolEndpointsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TaskStore.class)
    public TaskStore inMemoryTaskStore() {
        return new InMemoryTaskStore();
    }

    @Bean
    @ConditionalOnMissingBean(AugmentedPromptStore.class)
    public AugmentedPromptStore inMemoryAugmentedPromptStore() {
        return new InMemoryAugmentedPromptStore();
    }

    @Bean
    @ConditionalOnMissingBean(NotificationsPollingEndpoint.class)
    public NotificationsPollingEndpoint notificationsPollingEndpoint() {
        return new NotificationsPollingEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean(TasksEndpoint.class)
    public TasksEndpoint tasksEndpoint(TaskStore taskStore) {
        return new TasksEndpoint(taskStore);
    }

    @Bean
    @ConditionalOnMissingBean(AugmentedPromptEndpoint.class)
    public AugmentedPromptEndpoint augmentedPromptEndpoint(AugmentedPromptStore store) {
        return new AugmentedPromptEndpoint(store);
    }

    /**
     * Discover endpoint — uses the framework's
     * {@link com.ai.plug.core.spec.capabilities.WireServerCapabilities}
     * (protocol-2026-07-28 wire shape).
     */
    @Bean
    @ConditionalOnMissingBean(DiscoverEndpoint.class)
    public DiscoverEndpoint discoverEndpoint(
            ObjectProvider<McpSchema.ServerCapabilities> capabilitiesProvider,
            com.ai.plug.core.spec.capabilities.WireServerCapabilities wireServerCapabilities) {
        Supplier<Object> capsSupplier = () -> wireServerCapabilities;
        Supplier<Map<String, Object>> extSupplier = () -> Map.of();
        return new DiscoverEndpoint("server2mcp", "1.1.4-SNAPSHOT",
            capsSupplier, extSupplier);
    }

    @Bean
    @ConditionalOnMissingBean(com.ai.plug.core.spec.capabilities.WireServerCapabilities.class)
    public com.ai.plug.core.spec.capabilities.WireServerCapabilities wireServerCapabilities() {
        return com.ai.plug.core.spec.capabilities.WireServerCapabilities.full();
    }

    /** WebFlux configurer marker so Spring Boot picks up our controllers. */
    @Bean
    @ConditionalOnMissingBean
    public WebFluxConfigurer mcpProtocolEndpointsConfigurer() {
        return new WebFluxConfigurer() { };
    }

    @Bean
    @ConditionalOnMissingBean(MrtrSessionStore.class)
    public MrtrSessionStore inMemoryMrtrSessionStore() {
        return new InMemoryMrtrSessionStore();
    }

    @Bean
    @ConditionalOnMissingBean(JsonRpcRouter.class)
    public JsonRpcRouter jsonRpcRouter(
            DiscoverEndpoint discoverEndpoint,
            TaskStore taskStore,
            TasksEndpoint tasksEndpoint,
            AugmentedPromptEndpoint augmentedPromptEndpoint,
            NotificationsPollingEndpoint notificationsEndpoint,
            MrtrSessionStore mrtrStore) {
        JsonRpcRouter router = new JsonRpcRouter();
        JsonRpcRoutes.registerAll(router,
            discoverEndpoint,
            taskStore,
            tasksEndpoint,
            augmentedPromptEndpoint,
            notificationsEndpoint,
            mrtrStore);
        return router;
    }

    @Bean
    @ConditionalOnMissingBean(JsonRpcController.class)
    public JsonRpcController jsonRpcController(JsonRpcRouter router) {
        return new JsonRpcController(router);
    }

    /**
     * Reactive SSE controller — wires
     * {@link NotificationsPollingEndpoint#setListener} so that
     * every recorded event is broadcast to live SSE clients.
     */
    @Bean
    @ConditionalOnMissingBean(SseNotificationsController.class)
    public SseNotificationsController sseNotificationsController(
            NotificationsPollingEndpoint notifications) {
        SseNotificationsController controller = new SseNotificationsController(notifications);
        notifications.setListener(controller::broadcast);
        return controller;
    }

    // ---- HTTP legacy endpoints (kept as fallback for older clients) ----

    @Bean
    @ConditionalOnMissingBean(DiscoverController.class)
    public DiscoverController discoverController(DiscoverEndpoint discoverEndpoint) {
        return new DiscoverController(discoverEndpoint);
    }

    @Bean
    @ConditionalOnMissingBean(TasksController.class)
    public TasksController tasksController(TasksEndpoint tasksEndpoint) {
        return new TasksController(tasksEndpoint);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationsController.class)
    public NotificationsController notificationsController(NotificationsPollingEndpoint notifications) {
        return new NotificationsController(notifications);
    }

    @Bean
    @ConditionalOnMissingBean(AugmentedPromptsController.class)
    public AugmentedPromptsController augmentedPromptsController(AugmentedPromptEndpoint endpoint) {
        return new AugmentedPromptsController(endpoint);
    }
}
