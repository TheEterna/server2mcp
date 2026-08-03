package com.ai.plug.core.spec.discover;

import com.ai.plug.common.utils.JsonParser;
import com.ai.plug.core.spec.implementation.ServerInfoFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Framework-layer HTTP endpoint contract for {@code server/discover}.
 *
 * <p>SDK 2.0 has no JSON-RPC schema for {@code server/discover}, so this
 * endpoint is exposed at the HTTP layer (e.g. {@code GET /mcp/discover})
 * rather than over JSON-RPC. Downstream {@code server-boot-actuator} or
 * any HTTP transport can mount this contract directly.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   DiscoverEndpoint endpoint = new DiscoverEndpoint(
 *       "my-server", "1.0.0",
 *       () -> serverCapabilities,
 *       () -> extensions);
 *   String json = endpoint.handleJson();
 *   // {"protocolVersions":["2026-07-28"],"preferredVersion":"2026-07-28",
 *   //  "serverInfo":{"name":"my-server","version":"1.0.0"},...}
 * }</pre>
 *
 * @author han
 * @time 2026/8/3
 */
public final class DiscoverEndpoint {

    /** Protocol versions this server is willing to speak, in preference order. */
    public static final List<String> SUPPORTED_PROTOCOL_VERSIONS =
        List.of("2026-07-28", "2025-11-25");

    /** The version we'll negotiate to by default. */
    public static final String PREFERRED_PROTOCOL_VERSION = "2026-07-28";

    private final String serverName;
    private final String serverVersion;
    private final java.util.function.Supplier<Object> capabilitiesSupplier;
    private final java.util.function.Supplier<Map<String, Object>> extensionsSupplier;

    public DiscoverEndpoint(String serverName,
                             String serverVersion,
                             java.util.function.Supplier<Object> capabilitiesSupplier,
                             java.util.function.Supplier<Map<String, Object>> extensionsSupplier) {
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("serverName is required");
        }
        if (serverVersion == null || serverVersion.isBlank()) {
            throw new IllegalArgumentException("serverVersion is required");
        }
        if (capabilitiesSupplier == null) {
            throw new IllegalArgumentException("capabilitiesSupplier is required");
        }
        if (extensionsSupplier == null) {
            throw new IllegalArgumentException("extensionsSupplier is required");
        }
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.capabilitiesSupplier = capabilitiesSupplier;
        this.extensionsSupplier = extensionsSupplier;
    }

    /**
     * Convenience overload — no extensions.
     */
    public DiscoverEndpoint(String serverName,
                             String serverVersion,
                             java.util.function.Supplier<Object> capabilitiesSupplier) {
        this(serverName, serverVersion, capabilitiesSupplier, Map::of);
    }

    /**
     * Build the discover response as a plain {@link Map}, suitable for
     * any transport that serializes its own map (Jackson, JSON-B, etc.).
     */
    public Map<String, Object> handle() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("protocolVersions", SUPPORTED_PROTOCOL_VERSIONS);
        body.put("preferredVersion", PREFERRED_PROTOCOL_VERSION);
        body.put("serverInfo", ServerInfoFactory.create(serverName, serverVersion));
        body.put("capabilities", capabilitiesSupplier.get());
        Map<String, Object> ext = extensionsSupplier.get();
        if (ext != null && !ext.isEmpty()) {
            body.put("extensions", ext);
        }
        return body;
    }

    /** Convenience: serialize {@link #handle()} as a JSON string using
     *  the project's shared Jackson 3 mapper. */
    public String handleJson() throws java.io.IOException {
        return JsonParser.getObjectMapper().writeValueAsString(handle());
    }

    /** Negotiate the protocol version for a given client preferred version.
     *  Returns the client's preferred version if supported, otherwise the
     *  server's preferred version. */
    public static String negotiate(String clientPreferred) {
        if (clientPreferred == null || clientPreferred.isBlank()) {
            return PREFERRED_PROTOCOL_VERSION;
        }
        String result = VersionNegotiator.negotiate(clientPreferred, SUPPORTED_PROTOCOL_VERSIONS);
        return result != null ? result : PREFERRED_PROTOCOL_VERSION;
    }
}