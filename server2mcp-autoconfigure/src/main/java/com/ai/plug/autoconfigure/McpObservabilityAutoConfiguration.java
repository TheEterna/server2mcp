/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.autoconfigure;

import com.ai.plug.core.observability.McpTracer;
import com.ai.plug.core.observability.NoopMcpTracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers an {@link McpTracer} bean:
 * <ul>
 *   <li>by default a {@link NoopMcpTracer} (zero cost, no spans emitted);</li>
 *   <li>override by registering a custom {@code @Bean McpTracer} in user
 *       code — for example, a thin adapter that delegates to
 *       {@code io.opentelemetry.api.trace.Tracer#spanBuilder(String)}.</li>
 * </ul>
 *
 * <p>The OpenTelemetry-bridged implementation is intentionally not
 * shipped by api2mcp4j itself. Reason: forcing a hard
 * {@code opentelemetry-api} dependency would lock the framework to
 * one tracing vendor. The {@link McpTracer} SPI keeps the door open
 * for any vendor (OTel, Brave, homegrown) without adding a runtime
 * cost to deployments that don't care.
 *
 * <h2>How to enable real OTel spans</h2>
 * <ol>
 *   <li>Add to your app's pom: {@code io.opentelemetry:opentelemetry-api}
 *       and your preferred exporter (e.g. {@code opentelemetry-exporter-otlp};</li>
 *   <li>Define a {@code @Bean OpenTelemetry} using OTel's SDK setup;</li>
 *   <li>Define a {@code @Bean McpTracer} that adapts to your
 *       {@code OpenTelemetry} instance. See
 *       {@code docs/reference/observability.md} for a copy-pasteable
 *       implementation.</li>
 * </ol>
 */
@AutoConfiguration
public class McpObservabilityAutoConfiguration {

    /**
     * Default {@link McpTracer} — no-op unless the user supplies a
     * real bean. Conditional so any user-provided bean wins.
     */
    @Bean
    @ConditionalOnMissingBean(McpTracer.class)
    public McpTracer mcpTracer() {
        return new NoopMcpTracer();
    }

    /**
     * Sentinel: if a user adds {@code io.opentelemetry:opentelemetry-api}
     * to their classpath without also providing an {@link McpTracer}
     * bean, the framework stays on the no-op tracer but logs a one-line
     * warning so the operator notices. This avoids the silent case
     * where OTel is on the classpath but no spans are emitted.
     */
    @Bean
    @ConditionalOnClass(name = "io.opentelemetry.api.OpenTelemetry")
    @ConditionalOnMissingBean(McpTracer.class)
    public McpTracer openTelemetrySentinel() {
        org.slf4j.LoggerFactory.getLogger(McpObservabilityAutoConfiguration.class)
            .info("io.opentelemetry.api.OpenTelemetry is on the classpath but no McpTracer bean is registered. "
                + "Define a @Bean McpTracer to bridge to OTel. See docs/reference/observability.md.");
        return new NoopMcpTracer();
    }
}
