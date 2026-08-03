package com.ai.plug.core.spec.capabilities;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the {@link CapabilitiesJsonExporter} produces a wire
 * tree that matches the protocol-2026-07-28 {@code ServerCapabilities}
 * schema, with every 2026-07-28-only field present and at its
 * canonical default.
 *
 * @author han
 * @time 2026/8/3
 */
class CapabilitiesJsonExporterTest {

    private final CapabilitiesJsonExporter exporter = new CapabilitiesJsonExporter();

    @Test
    void full_emitsEvery2026Field() {
        Map<String, Object> wire = exporter.toMap(WireServerCapabilities.full());
        // Top-level keys
        assertThat(wire).containsKeys("tools", "resources", "prompts",
            "completions", "experimental");
        // 2026-07-28-only fields
        assertThat((Map<String, Object>) wire.get("tools"))
            .containsEntry("listChanged", true)
            .containsEntry("subscription", true);
        assertThat((Map<String, Object>) wire.get("resources"))
            .containsEntry("subscribe", true)
            .containsEntry("listChanged", true);
        assertThat((Map<String, Object>) wire.get("prompts"))
            .containsEntry("listChanged", true);
        assertThat((Map<String, Object>) wire.get("completions"))
            .containsEntry("listChanged", true);
        // Experimental extension carrier
        Map<String, Object> experimental = (Map<String, Object>) wire.get("experimental");
        assertThat(experimental).containsKey("io.modelcontextprotocol/tasks");
    }

    @Test
    void full_omitsEmptyLogging() {
        // Logging is empty in full(); @JsonInclude.NON_NULL semantics:
        // empty maps are still rendered. Verify the exporter's policy.
        Map<String, Object> wire = exporter.toMap(WireServerCapabilities.full());
        // Per our Map.of() default, logging is absent (the exporter
        // skips empty maps — see toMap implementation).
        assertThat(wire).doesNotContainKey("logging");
    }

    @Test
    void custom_caps_passThroughVerbatim() {
        WireServerCapabilities caps = new WireServerCapabilities(
            new WireServerCapabilities.Tools(false, true),
            new WireServerCapabilities.Resources(false, false),
            new WireServerCapabilities.Prompts(false),
            new WireServerCapabilities.Completions(false),
            null,
            null
        );
        Map<String, Object> wire = exporter.toMap(caps);
        assertThat((Map<String, Object>) wire.get("tools"))
            .containsEntry("listChanged", false)
            .containsEntry("subscription", true);
        assertThat((Map<String, Object>) wire.get("resources"))
            .containsEntry("subscribe", false)
            .containsEntry("listChanged", false);
        assertThat(wire).doesNotContainKey("experimental");
        assertThat(wire).doesNotContainKey("logging");
    }

    @Test
    void experimental_mapRoundTrip() {
        Map<String, Object> experimental = Map.of(
            "io.modelcontextprotocol/tasks",
            Map.of("subscribe", true),
            "custom", "value");
        WireServerCapabilities caps = new WireServerCapabilities(
            null, null, null, null, null, experimental);
        Map<String, Object> wire = exporter.toMap(caps);
        assertThat(wire.get("experimental")).isEqualTo(experimental);
    }
}