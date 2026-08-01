package com.ai.plug.core.spec.integration;

import com.ai.plug.core.spec.implementation.ServerInfoFactory;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.util.annotation.Nullable;

import java.util.Map;

/**
 * Announce helper — assemble a server announcement payload that combines
 * server identity + advertised capabilities + optional extensions into a
 * single structure. Designed to be sent as a single JSON-RPC
 * notifications/message during server startup (or on demand).
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var announcement = ServerAnnounce.builder()
 *       .info(ServerInfoFactory.create("my-mcp", "1.0", "My MCP", "Hello world"))
 *       .capabilities(ServerCapabilitiesFactory.withListChangedAll())
 *       .extension("io.modelcontextprotocol/tasks", Map.of("version", "draft"))
 *       .build();
 *   // Serialize via your transport — McpResultWriter-style
 *   String json = ServerAnnounce.toJson(announcement);
 * }</pre>
 *
 * <p>SDK 2.0 does not yet expose a {@code server/discover} RPC or an
 * announcement notification. This struct lets integrators build the payload
 * client-side and emit it through whatever mechanism their transport supports
 * (e.g. a dedicated SSE stream or a websocket broadcast).
 */
public record ServerAnnounce(
        McpSchema.Implementation serverInfo,
        McpSchema.ServerCapabilities capabilities,
        Map<String, Object> extensions
) {

    public ServerAnnounce {
        if (serverInfo == null) {
            throw new IllegalArgumentException("serverInfo is required");
        }
        if (capabilities == null) {
            throw new IllegalArgumentException("capabilities is required");
        }
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }

    /**
     * Builder-style factory. Prefer over direct constructor for readability.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Build a minimal announce from a name + version — uses
     * {@link ServerInfoFactory} for the identity and the standard
     * "listChanged all" capability set.
     */
    public static ServerAnnounce minimal(String name, String version) {
        return new ServerAnnounce(
            ServerInfoFactory.create(name, version),
            com.ai.plug.core.spec.capabilities.ServerCapabilitiesFactory.withListChangedAll(),
            Map.of());
    }

    /**
     * Serialize the announce to a JSON string using the project's shared
     * Jackson 3 mapper. Use this for sending through a transport that
     * accepts pre-serialized bytes.
     */
    public String toJson() throws java.io.IOException {
        return com.ai.plug.common.utils.JsonParser.getObjectMapper()
            .writeValueAsString(this);
    }

    /** Static convenience for {@link #toJson()} without a reference. */
    public static String toJson(ServerAnnounce announce) throws java.io.IOException {
        return announce.toJson();
    }

    /** Builder for {@link ServerAnnounce}. */
    public static final class Builder {
        @Nullable
        private McpSchema.Implementation info;
        @Nullable
        private McpSchema.ServerCapabilities caps;
        private final java.util.LinkedHashMap<String, Object> ext = new java.util.LinkedHashMap<>();

        public Builder info(McpSchema.Implementation info) {
            this.info = info;
            return this;
        }

        public Builder capabilities(McpSchema.ServerCapabilities caps) {
            this.caps = caps;
            return this;
        }

        public Builder extension(String key, Object value) {
            this.ext.put(key, value);
            return this;
        }

        public Builder extensions(Map<String, Object> ext) {
            this.ext.putAll(ext);
            return this;
        }

        public ServerAnnounce build() {
            if (info == null) {
                throw new IllegalStateException("info is required");
            }
            if (caps == null) {
                throw new IllegalStateException("capabilities is required");
            }
            return new ServerAnnounce(info, caps, ext);
        }
    }
}