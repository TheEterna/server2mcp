package com.ai.plug.core.spec.capabilities;

/**
 * Spring-style bridge contract for {@link CapabilitiesHealthReport} into
 * a Spring Boot Actuator health indicator. Avoids the spring-boot-actuator
 * dependency in core; the downstream server-boot-actuator module provides
 * the actual {@code HealthIndicator} that delegates to this contract.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;Bean
 *   public CapabilitiesHealthSpringActuator healthActuator(McpSyncServer server) {
 *       return new CapabilitiesHealthSpringActuator(
 *           new CapabilitiesHealthReportActuator(() -> server.spec().capabilities()));
 *   }
 * </pre>
 *
 * <p>Downstream {@code server-boot-actuator} module exposes this as
 * {@code /actuator/health/mcp-capabilities}. The contract here is the
 * payload shape contract; HTTP wiring lives downstream.
 */
public class CapabilitiesHealthSpringActuator {

    private final CapabilitiesHealthReportActuator inner;

    public CapabilitiesHealthSpringActuator(CapabilitiesHealthReportActuator inner) {
        if (inner == null) {
            throw new IllegalArgumentException("inner is required");
        }
        this.inner = inner;
    }

    /**
     * Adapter: produce a Spring-style (health, details) pair.
     * Returned as a simple record so this module stays Spring-free.
     */
    public Health health() {
        CapabilitiesHealthReport report = inner.currentHealth();
        if (report.healthy()) {
            return new Health("UP", report);
        }
        return new Health("DOWN", report);
    }

    /** Spring-style health envelope (without importing Spring). */
    public record Health(String status, CapabilitiesHealthReport details) {
        public boolean isUp() { return "UP".equals(status); }
    }
}