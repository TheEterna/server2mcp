package com.ai.plug.core.spec.discover;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * {@code server/discover} RPC response schema — MCP protocol 2026-07-28 SEP-2575.
 * <p>
 * Servers MUST implement this RPC to advertise their supported protocol versions,
 * capabilities, and identity. Clients MAY call it before any other request for
 * up-front version selection, or use it as a backward-compatibility probe on
 * STDIO transports.
 * <p>
 * SDK 2.0 ships no {@code server/discover} schema (verified by grep). This
 * class owns the entire wire payload; JSON-RPC routing is the transport's job.
 *
 * @author han
 * @time 2026/8/1 02:02
 */
public final class DiscoverTypes {

    private DiscoverTypes() {
    }

    /**
     * The {@code server/discover} response payload.
     *
     * <p>Wire shape (top level):
     * <pre>{@code
     * {
     *   "protocolVersions": ["2025-11-25", "2026-07-28"],
     *   "preferredVersion": "2026-07-28",
     *   "serverInfo": { "name": "my-mcp", "version": "2.0.0", "title": "...",
     *                   "description": "...", "icons": [...], "websiteUrl": "..." },
     *   "capabilities": { "tools": {...}, "resources": {...}, "prompts": {...},
     *                     "experimental": {...}, "extensions": {...} },
     *   "instructions": "...",
     *   "meta": { ... }
     * }
     * }</pre>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DiscoverResult(
        List<String> protocolVersions,
        String preferredVersion,
        ServerIdentity serverInfo,
        Capabilities capabilities,
        String instructions,
        Map<String, Object> meta
    ) {

        public DiscoverResult {
            if (protocolVersions == null || protocolVersions.isEmpty()) {
                throw new IllegalArgumentException("protocolVersions must contain at least one entry");
            }
            if (preferredVersion == null || preferredVersion.isBlank()) {
                throw new IllegalArgumentException("preferredVersion is required");
            }
            if (!protocolVersions.contains(preferredVersion)) {
                throw new IllegalArgumentException(
                    "preferredVersion '" + preferredVersion + "' must be in protocolVersions");
            }
            if (serverInfo == null) {
                throw new IllegalArgumentException("serverInfo is required");
            }
        }

        /**
         * Builder-style factory; the {@code capabilities} field is optional on the wire.
         */
        public static DiscoverResult of(List<String> protocolVersions, String preferredVersion,
                                        ServerIdentity serverInfo) {
            return new DiscoverResult(protocolVersions, preferredVersion, serverInfo, null, null, null);
        }

        public static DiscoverResult of(List<String> protocolVersions, String preferredVersion,
                                        ServerIdentity serverInfo, Capabilities capabilities) {
            return new DiscoverResult(protocolVersions, preferredVersion, serverInfo, capabilities, null, null);
        }
    }

    /**
     * Server identity for {@code server/discover}. Mirrors {@code McpSchema.Implementation}
     * shape (name + version mandatory; title / description / icons / websiteUrl
     * optional per protocol 2025-11-25 server-identity extensions).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ServerIdentity(
        String name,
        String version,
        String title,
        String description,
        List<Map<String, Object>> icons,
        String websiteUrl
    ) {

        public ServerIdentity {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("serverInfo.name is required");
            }
            if (version == null || version.isBlank()) {
                throw new IllegalArgumentException("serverInfo.version is required");
            }
        }

        public static ServerIdentity of(String name, String version) {
            return new ServerIdentity(name, version, null, null, null, null);
        }

        public static ServerIdentity of(String name, String version, String title, String description) {
            return new ServerIdentity(name, version, title, description, null, null);
        }
    }

    /**
     * Capability declaration. Mirrors the protocol's capability schema with a
     * free-form {@code experimental} map and an {@code extensions} map
     * (added in 2026-07-28).
     * <p>
     * The four canonical capability buckets ({@link #tools}, {@link #resources},
     * {@link #prompts}, {@link #logging}) are typed as boolean maps for forward
     * compatibility — the protocol defines flags like {@code listChanged} and
     * {@code subscribe} that may evolve without breaking the wire.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Capabilities(
        Map<String, Object> tools,
        Map<String, Object> resources,
        Map<String, Object> prompts,
        Map<String, Object> logging,
        Map<String, Object> experimental,
        @JsonProperty("extensions") Map<String, Object> extensions
    ) {

        public Capabilities {
            // At least one capability bucket must be present — a server that does
            // nothing should not expose server/discover at all.
            boolean anyPresent = tools != null || resources != null || prompts != null
                || logging != null || experimental != null || extensions != null;
            if (!anyPresent) {
                throw new IllegalArgumentException("at least one capability bucket is required");
            }
        }

        /**
         * Convenience for the most common case: server supports tools + resources +
         * prompts with {@code listChanged=true} on each.
         */
        public static Capabilities allWithListChanged() {
            return new Capabilities(
                Map.of("listChanged", true),
                Map.of("subscribe", true, "listChanged", true),
                Map.of("listChanged", true),
                null, null, null);
        }

        /**
         * Server advertises one or more 2026-07-28 protocol extensions (e.g.
         * {@code io.modelcontextprotocol/tasks}). The {@code extensions} map is
         * intentionally free-form — the protocol permits any URI-namespaced key.
         */
        public static Capabilities withExtensions(Map<String, Object> extensions) {
            return new Capabilities(null, null, null, null, null,
                extensions == null ? Map.of() : extensions);
        }
    }

    /**
     * Request payload for `server/discover` — what the client sends when
     * probing server capabilities. Records the client's preferred protocol
     * version so the server can pick the best match.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DiscoverRequest(String preferredProtocol) {

        public DiscoverRequest {
            if (preferredProtocol == null || preferredProtocol.isBlank()) {
                throw new IllegalArgumentException(
                    "preferredProtocol is required (e.g. \"2025-11-25\" or \"2026-07-28\")");
            }
        }

        public static DiscoverRequest of(String preferredProtocol) {
            return new DiscoverRequest(preferredProtocol);
        }
    }
}