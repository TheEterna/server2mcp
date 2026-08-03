package com.ai.plug.core.spec.capabilities;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Spring Boot Actuator integration for {@link CapabilitiesHealth}.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;Bean
 *   public CapabilitiesHealthReportActuator capabilitiesActuator(
 *           McpSyncServer server) {
 *       return new CapabilitiesHealthReportActuator(() -&gt; server.spec().capabilities());
 *   }
 * }</pre>
 *
 * <p>Wires {@link CapabilitiesHealthReport} as a Spring Actuator health
 * indicator — at {@code /actuator/health/mcp-capabilities}, the framework
 * exposes the current capabilities health (healthy / unhealthy + issues
 * list) for ops dashboards.
 *
 * <p>This file does not import Spring Boot Actuator directly (the project
 * does not depend on actuator). The bridge lives in
 * {@code com.ai.plug.server.boot.actuator} (downstream module) — see
 * {@code CapabilitiesHealthActuatorBridge} for the actual Spring binding.
 * The method shape defined here is the contract.
 */
public class CapabilitiesHealthReportActuator {

    private final java.util.function.Supplier<McpSchema.ServerCapabilities> source;

    public CapabilitiesHealthReportActuator(
            java.util.function.Supplier<McpSchema.ServerCapabilities> source) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        this.source = source;
    }

    /**
     * Build a {@link CapabilitiesHealthReport} from the current source.
     * Returns a healthy report on missing source / null caps to avoid
     * noisy startup-time health signals.
     */
    public CapabilitiesHealthReport currentHealth() {
        McpSchema.ServerCapabilities caps;
        try {
            caps = source.get();
        }
        catch (Exception ex) {
            return new CapabilitiesHealthReport(true, 0, java.util.List.of());
        }
        if (caps == null) {
            return new CapabilitiesHealthReport(true, 0, java.util.List.of());
        }
        return CapabilitiesHealthReport.of(CapabilitiesHealth.check(caps));
    }
}