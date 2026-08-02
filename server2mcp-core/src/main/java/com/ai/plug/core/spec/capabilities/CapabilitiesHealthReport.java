package com.ai.plug.core.spec.capabilities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * JSON-serializable health report for {@link CapabilitiesHealth}. Wraps the
 * internal {@link CapabilitiesHealth.Report} so it can be exposed via
 * Spring Boot Actuator, REST endpoints, or any JSON consumer.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var health = CapabilitiesHealth.check(caps);
 *   var report = CapabilitiesHealthReport.of(health);
 *   String json = objectMapper.writeValueAsString(report);
 * }</pre>
 *
 * <p>Wire format (non-null fields only):
 * <pre>{@code
 *   {
 *     "healthy": false,
 *     "issueCount": 2,
 *     "issues": ["missing required flag: x", "required flag is false: y"]
 *   }
 * }</pre>
 */
public record CapabilitiesHealthReport(
        @JsonProperty("healthy") boolean healthy,
        @JsonProperty("issueCount") int issueCount,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) @JsonProperty("issues") List<String> issues
) {

    public static CapabilitiesHealthReport of(CapabilitiesHealth.Report report) {
        if (report == null) {
            return new CapabilitiesHealthReport(true, 0, List.of());
        }
        return new CapabilitiesHealthReport(
            report.isHealthy(),
            report.issues().size(),
            report.issues());
    }
}