package com.ai.plug.core.spec.capabilities;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serialises a {@link WireServerCapabilities} to a plain
 * {@code Map<String,Object>} tree matching the protocol-2026-07-28 wire
 * shape. The exporter is intentionally Jackson-free so it can live in
 * {@code server2mcp-core} (which doesn't depend on a Jackson version);
 * the actual JSON encoding is delegated to whatever the consuming layer
 * brings (Spring Boot 4.x brings Jackson 3 by default).
 *
 * <p>This single export seam is called by the {@code DiscoverEndpoint},
 * the {@code server/discover} JSON-RPC route, the actuator health
 * endpoint, and the snapshot tooling. When SDK ≥ 3.0.0 ships its native
 * {@code ServerCapabilities} record, we keep this exporter as a stable
 * wire-format fallback and only swap the upstream producer.
 *
 * @author han
 * @time 2026/8/3
 */
public final class CapabilitiesJsonExporter {

    public CapabilitiesJsonExporter() {
    }

    /**
     * Serialise {@code caps} as a {@code Map<String,Object>} tree —
     * the canonical 2026-07-28 wire shape, with sub-objects for
     * {@code tools} / {@code resources} / {@code prompts} /
     * {@code completions} and a flat {@code experimental} map.
     */
    public Map<String, Object> toMap(WireServerCapabilities caps) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (caps.tools() != null) out.put("tools", toolsToMap(caps.tools()));
        if (caps.resources() != null) out.put("resources", resourcesToMap(caps.resources()));
        if (caps.prompts() != null) out.put("prompts", promptsToMap(caps.prompts()));
        if (caps.completions() != null) out.put("completions", completionsToMap(caps.completions()));
        if (caps.logging() != null && !caps.logging().isEmpty()) out.put("logging", caps.logging());
        if (caps.experimental() != null && !caps.experimental().isEmpty()) {
            out.put("experimental", caps.experimental());
        }
        return out;
    }

    private static Map<String, Object> toolsToMap(WireServerCapabilities.Tools t) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (t.listChanged() != null) m.put("listChanged", t.listChanged());
        if (t.subscription() != null) m.put("subscription", t.subscription());
        return m;
    }

    private static Map<String, Object> resourcesToMap(WireServerCapabilities.Resources r) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (r.subscribe() != null) m.put("subscribe", r.subscribe());
        if (r.listChanged() != null) m.put("listChanged", r.listChanged());
        return m;
    }

    private static Map<String, Object> promptsToMap(WireServerCapabilities.Prompts p) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (p.listChanged() != null) m.put("listChanged", p.listChanged());
        return m;
    }

    private static Map<String, Object> completionsToMap(WireServerCapabilities.Completions c) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (c.listChanged() != null) m.put("listChanged", c.listChanged());
        return m;
    }
}