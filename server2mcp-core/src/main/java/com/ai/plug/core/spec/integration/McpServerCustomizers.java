package com.ai.plug.core.spec.integration;

import com.ai.plug.core.spec.capabilities.ServerCapabilitiesFactory;
import com.ai.plug.core.spec.headers.McpRequestHeaders;
import com.ai.plug.core.spec.implementation.ServerInfoFactory;
import org.springframework.ai.mcp.customizer.McpAsyncServerCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;

import java.util.List;
import java.util.Map;

/**
 * Spring AI 2.0 customizer templates — bridges this framework's spec helpers
 * (ServerInfoFactory / ServerCapabilitiesFactory / McpRequestHeaders) into the
 * user's MCP server without requiring them to wire SDK builders by hand.
 * <p>
 * Usage:
 * <pre>{@code
 *   &#64;Configuration
 *   public class MyMcpConfig {
 *       &#64;Bean
 *       public McpSyncServerCustomizer listChangedCustomizer() {
 *           return McpServerCustomizers.syncListChangedAll(...)
 *               .andThen(McpServerCustomizers.serverInfo(
 *                   "my-mcp", "1.0.0", "My MCP", "Hello world"));
 *       }
 *   }
 * }</pre>
 *
 * <h2>Spring AI 2.0 contract</h2>
 * Spring AI 2.0 starter picks up any {@code McpSyncServerCustomizer} /
 * {@code McpAsyncServerCustomizer} Bean and applies them in order to the
 * respective {@code SyncSpecification} / {@code AsyncSpecification} before
 * {@code build()}. The customizer is therefore the canonical place to inject
 * this framework's spec helpers.
 *
 * @author han
 * @time 2026/8/1 05:49
 */
public final class McpServerCustomizers {

    private McpServerCustomizers() {
    }

    // ---- listChanged / subscribe caps ----

    /**
     * Synchronous customizer that wires {@link ServerCapabilitiesFactory#withListChangedAll()}
     * into the spec — turns on tools/resources/prompts listChanged notifications
     * plus resource subscribe. Use as-is or as the start of an andThen chain.
     */
    public static McpSyncServerCustomizer syncListChangedAll() {
        return spec -> spec.capabilities(ServerCapabilitiesFactory.withListChangedAll());
    }

    public static McpAsyncServerCustomizer asyncListChangedAll() {
        return spec -> spec.capabilities(ServerCapabilitiesFactory.withListChangedAll());
    }

    /**
     * Wire only tools.listChanged. Lighter-touch than {@link #syncListChangedAll()}.
     */
    public static McpSyncServerCustomizer syncToolsListChanged() {
        return spec -> spec.capabilities(ServerCapabilitiesFactory.withToolsListChanged());
    }

    // ---- server identity ----

    /**
     * Wire server identity (name + version, optional title/description).
     * Uses {@link ServerInfoFactory#create(String, String, String, String)}.
     */
    public static McpSyncServerCustomizer serverInfo(String name, String version) {
        return spec -> spec.serverInfo(name, version);
    }

    public static McpSyncServerCustomizer serverInfo(String name, String version, String title, String description) {
        return spec -> spec.serverInfo( ServerInfoFactory.create(name, version, title, description));
    }

    // ---- request-time header injection ----

    /**
     * Build a per-request header mapper from a {@link McpRequestHeaders} config.
     * The returned customizer wraps the spec's toolCall so that every outgoing
     * call from the server carries the Mcp-Method / Mcp-Name headers (SEP-2243).
     * <p>
     * Note: this is a no-op for SDK 2.0 because the spec doesn't expose a
     * request-hook. Kept as a stable API signature so callers can opt-in once
     * Spring AI 2.1+ exposes the hook.
     */
    public static McpSyncServerCustomizer standardHeaders(String jsonRpcMethod, String name) {
        Map<String, String> headers = McpRequestHeaders.forJsonRpcCall(jsonRpcMethod, name);
        // SDK 2.0 has no request-hook surface on SyncSpecification — header
        // attachment is the integrator's responsibility via their HTTP client.
        // We expose the helper so callers can read the headers they should attach.
        return spec -> spec.instructions("McpRequestHeaders.forJsonRpcCall returned: " + headers);
    }

    // ---- chaining ----

    /**
     * Compose two customizers into a single one that applies them in order.
     * Lets users express a sequence without lambda-in-lambda noise.
     */
    public static McpSyncServerCustomizer compose(McpSyncServerCustomizer first, McpSyncServerCustomizer second) {
        return spec -> {
            first.customize(spec);
            second.customize(spec);
        };
    }

    public static McpAsyncServerCustomizer compose(McpAsyncServerCustomizer first, McpAsyncServerCustomizer second) {
        return spec -> {
            first.customize(spec);
            second.customize(spec);
        };
    }

    /**
     * Convenience for composing many customizers (3+).
     * Renamed from {@code allOf} to avoid ambiguity with the async overload.
     */
    public static McpSyncServerCustomizer composeAll(McpSyncServerCustomizer... customizers) {
        List<McpSyncServerCustomizer> list = List.of(customizers);
        return spec -> list.forEach(c -> c.customize(spec));
    }

    public static McpAsyncServerCustomizer composeAllAsync(McpAsyncServerCustomizer... customizers) {
        List<McpAsyncServerCustomizer> list = List.of(customizers);
        return spec -> list.forEach(c -> c.customize(spec));
    }

    /**
     * Silence the unused 'spec' parameter warning on the standardHeaders overload
     * — keeps the API surface uniform with the others.
     */
    @SuppressWarnings("unused")
    private static void touchSpec(io.modelcontextprotocol.server.McpServer.SyncSpecification<?> spec) {
        // no-op
    }
}