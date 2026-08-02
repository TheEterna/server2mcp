package com.ai.plug.core.spec.capabilities;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Versions and identity for the framework's wire-layer metadata.
 *
 * <h2>组成</h2>
 * - {@link #FRAMEWORK_VERSION} — semantic version of this framework
 *   (read from manifest at build time, hard-coded as fallback for unit tests)
 * - {@link #PROTOCOL_VERSIONS} — list of protocol versions this framework
 *   can emit at the wire layer
 * - {@link #wireVersionMap()} — convenience: map of versions to put in
 *   {@code ServerCapabilities.experimental()} as
 *   {@code "io.modelcontextprotocol/api2mcp4j"} (this framework's identity)
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var caps = WireSchemaExporter.fullCapabilitiesWithExtensions(
 *       CapabilityVersioning.wireVersionMap());
 *   // caps.experimental() now carries:
 *   //   "io.modelcontextprotocol/api2mcp4j": { "version": "1.1.4", ... }
 * }</pre>
 */
public final class CapabilityVersioning {

    /** Current framework version. Updated at release time. */
    public static final String FRAMEWORK_VERSION = "1.1.4-SNAPSHOT";

    /** Protocol versions this framework supports. */
    public static final java.util.List<String> PROTOCOL_VERSIONS = java.util.List.of(
        "2025-11-25", "2026-07-28"
    );

    /** Standard key under which the framework's identity is published. */
    public static final String FRAMEWORK_KEY = "io.modelcontextprotocol/api2mcp4j";

    private CapabilityVersioning() {
    }

    /**
     * Build the framework's wire identity map. Always returns a new
     * LinkedHashMap (predictable iteration order helps golden-file tests).
     */
    public static Map<String, Object> wireVersionMap() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("version", FRAMEWORK_VERSION);
        info.put("protocolVersions", PROTOCOL_VERSIONS);
        info.put("wireFields", java.util.List.of(
            "resultType", "ttlMs", "cacheScope", "cacheWrapperKey",
            "nextCursor", "inputRequests", "requestState", "taskHandle"
        ));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(FRAMEWORK_KEY, info);
        return out;
    }

    /**
     * Build a {@link McpSchema.ServerCapabilities} with both listChanged all
     * and the framework's wire identity. Convenience for Spring AI customizer
     * Bean definitions:
     *
     * <pre>{@code
     *   &#64;Bean
     *   public McpSyncServerCustomizer capabilities() {
     *       return spec -&gt; spec.capabilities(CapabilityVersioning.fullCapabilities());
     *   }
     * }</pre>
     */
    public static McpSchema.ServerCapabilities fullCapabilities() {
        return com.ai.plug.core.spec.integration.WireSchemaExporter
            .fullCapabilitiesWithExtensions(wireVersionMap());
    }
}