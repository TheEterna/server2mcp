/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @file: ProtocolEndpointsAutoConfiguration.java
 * @module: server2mcp-starter-webmvc
 * @layer: starter
 * @since: 2026/8/3
 * @author: han
 * @updated: 2026-08-03
 *
 * Spring Boot auto-configuration that mounts the framework's protocol-2026-07-28
 * HTTP contracts (Discover, Tasks, Notifications, AugmentedPrompts) at
 * {@code /mcp/...} endpoints.
 *
 * <p>This bridges the gap between Java SDK 2.0 (no JSON-RPC schema for the
 * new RPC routes) and the MCP 2026-07-28 protocol. When Java SDK ≥ 3.0.0
 * ships native routes, the controllers remain as a fallback for older
 * clients — no business-code change required.
 */

package com.ai.plug.starter.webmvc;

import com.ai.plug.core.spec.change.NotificationsPollingEndpoint;
import com.ai.plug.core.spec.discover.DiscoverEndpoint;
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
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Auto-configuration for the MCP protocol HTTP endpoints.
 *
 * <p>Wires the four framework-layer endpoints into Spring's application
 * context as singletons, then exposes them through dedicated MVC
 * controllers under {@code /mcp/...}.
 *
 * <h2>Endpoints exposed</h2>
 * <ul>
 *   <li>{@code GET /mcp/discover} → {@link DiscoverController}</li>
 *   <li>{@code GET /mcp/tasks} / {@code /mcp/tasks/{id}} / {@code POST /mcp/tasks/{id}/cancel}
 *       → {@link TasksController}</li>
 *   <li>{@code GET /mcp/tasks/{id}/augmented-prompts} / {@code POST /mcp/tasks/{id}/augmented-prompts/drain}
 *       → {@link AugmentedPromptsController}</li>
 *   <li>{@code GET /mcp/notifications} → {@link NotificationsController}</li>
 * </ul>
 *
 * <p>All four backing stores are exposed as Spring beans; user code can
 * replace them with Redis / JDBC implementations by registering a
 * {@code @Primary} bean of the corresponding type.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ProtocolEndpointsAutoConfiguration {

    /** Default in-memory task store. Override via {@code @Primary @Bean}. */
    @Bean
    @ConditionalOnMissingBean(TaskStore.class)
    public TaskStore inMemoryTaskStore() {
        return new InMemoryTaskStore();
    }

    /** Default in-memory augmented-prompt store. Override via {@code @Primary @Bean}. */
    @Bean
    @ConditionalOnMissingBean(AugmentedPromptStore.class)
    public AugmentedPromptStore inMemoryAugmentedPromptStore() {
        return new InMemoryAugmentedPromptStore();
    }

    /** Shared notifications polling endpoint (mutable). */
    @Bean
    @ConditionalOnMissingBean(NotificationsPollingEndpoint.class)
    public NotificationsPollingEndpoint notificationsPollingEndpoint() {
        return new NotificationsPollingEndpoint();
    }

    /** Tasks endpoint backed by the configured {@link TaskStore}. */
    @Bean
    @ConditionalOnMissingBean(TasksEndpoint.class)
    public TasksEndpoint tasksEndpoint(TaskStore taskStore) {
        return new TasksEndpoint(taskStore);
    }

    /** Augmented-prompts endpoint backed by the configured store. */
    @Bean
    @ConditionalOnMissingBean(AugmentedPromptEndpoint.class)
    public AugmentedPromptEndpoint augmentedPromptEndpoint(AugmentedPromptStore store) {
        return new AugmentedPromptEndpoint(store);
    }

    /**
     * Discover endpoint — needs a server name + version + capability source.
     * Defaults to the values injected via {@code spring.ai.mcp.server.info}
     * or sensible fallbacks. Override by registering a custom bean.
     */
    @Bean
    @ConditionalOnMissingBean(DiscoverEndpoint.class)
    public DiscoverEndpoint discoverEndpoint(
            ObjectProvider<McpSchema.ServerCapabilities> capabilitiesProvider) {
        Supplier<Object> capsSupplier = () -> {
            McpSchema.ServerCapabilities caps = capabilitiesProvider.getIfAvailable();
            return caps == null
                ? io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.builder().build()
                : caps;
        };
        Supplier<Map<String, Object>> extSupplier = () -> Map.of();
        return new DiscoverEndpoint("server2mcp", "1.1.4-SNAPSHOT",
            capsSupplier, extSupplier);
    }

    /** MVC configurer marker so Spring Boot picks up our controllers. */
    @Bean
    @ConditionalOnMissingBean
    public WebMvcConfigurer mcpProtocolEndpointsConfigurer() {
        return new WebMvcConfigurer() { };
    }
}