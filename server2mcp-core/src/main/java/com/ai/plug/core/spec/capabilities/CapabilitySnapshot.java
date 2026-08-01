package com.ai.plug.core.spec.capabilities;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;
import java.util.TreeMap;

/**
 * Snapshot utility for {@link McpSchema.ServerCapabilities} — captures a
 * capabilities object's flag set into a comparable map for golden-file
 * testing / diff-based regression detection.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   // Capture a snapshot from a customizer-applied spec
 *   var snap = CapabilitySnapshot.from(capabilities);
 *
 *   // Use in a golden-file test
 *   assertThat(snap.flags()).isEqualTo(expectedGoldenMap);
 * </pre>
 *
 * <p>Flattens the nested record tree into a single Map<String, Boolean>
 * for easy equality assertions. Use {@link Map#equals(Object)} for
 * golden-file matching — TreeMap gives stable ordering.
 */
public record CapabilitySnapshot(Map<String, Boolean> flags) {

    public CapabilitySnapshot {
        // Defensive copy — caller cannot mutate the snapshot after construction
        flags = Map.copyOf(flags);
    }

    /** Build a snapshot from an {@link McpSchema.ServerCapabilities}. */
    public static CapabilitySnapshot from(McpSchema.ServerCapabilities caps) {
        // Use TreeMap for deterministic iteration order (helps golden-file
        // textual diffs)
        Map<String, Boolean> flags = new TreeMap<>();
        if (caps.tools() != null && caps.tools().listChanged() != null) {
            flags.put("tools.listChanged", caps.tools().listChanged());
        }
        if (caps.resources() != null) {
            if (caps.resources().listChanged() != null) {
                flags.put("resources.listChanged", caps.resources().listChanged());
            }
            if (caps.resources().subscribe() != null) {
                flags.put("resources.subscribe", caps.resources().subscribe());
            }
        }
        if (caps.prompts() != null && caps.prompts().listChanged() != null) {
            flags.put("prompts.listChanged", caps.prompts().listChanged());
        }
        if (caps.logging() != null) {
            flags.put("logging", true);
        }
        if (caps.completions() != null) {
            flags.put("completions", true);
        }
        if (caps.experimental() != null) {
            flags.put("extensions", true);
        }
        return new CapabilitySnapshot(flags);
    }

    /**
     * Compare two snapshots for semantic equality. Returns an empty string
     * when they match, or a human-readable diff otherwise.
     */
    public String diff(CapabilitySnapshot other) {
        java.util.Map<String, Boolean> mine = this.flags;
        java.util.Map<String, Boolean> theirs = other.flags;
        StringBuilder sb = new StringBuilder();
        for (var key : new TreeMap<>(mine).keySet()) {
            if (!theirs.containsKey(key)) {
                sb.append("- removed: ").append(key).append("=").append(mine.get(key)).append("\n");
            }
            else if (!theirs.get(key).equals(mine.get(key))) {
                sb.append("~ changed: ").append(key).append(": ")
                    .append(mine.get(key)).append(" -> ").append(theirs.get(key)).append("\n");
            }
        }
        for (var key : theirs.keySet()) {
            if (!mine.containsKey(key)) {
                sb.append("+ added: ").append(key).append("=").append(theirs.get(key)).append("\n");
            }
        }
        return sb.toString();
    }
}