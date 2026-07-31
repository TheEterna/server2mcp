package com.ai.plug.core.spec.integration;

import com.ai.plug.core.spec.capabilities.ServerCapabilitiesFactory;
import org.springframework.ai.mcp.customizer.McpAsyncServerCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bundles {@link McpServerCustomizers}'s helpers into a single "wire everything"
 * configuration point. Designed as a Spring {@code @Bean} so a user can drop
 * it into their config and get a fully wire-compliant MCP server with zero
 * code:
 *
 * <pre>{@code
 *   &#64;Bean
 *   public McpSyncServerCustomizer wireSchemaExporter() {
 *       return WireSchemaExporter.syncAll();
 *   }
 * }</pre>
 *
 * What you get when you register it:
 * <ul>
 *   <li>{@link ServerCapabilitiesFactory#withListChangedAll()} — tools /
 *       resources / prompts listChanged, resources subscribe</li>
 *   <li>Default tool / resource / prompt filter set (passthrough — relies on
 *       Spring AI defaults)</li>
 * </ul>
 *
 * Plus a sibling bean for async servers. Combine with
 * {@link com.ai.plug.core.spec.change.McpToolChangeNotifier} on a
 * ScheduledExecutorService to fire change notifications in real time.
 *
 * <h2>公开 API 扩展点</h2>
 * If you need custom title / description on the server identity, register a
 * second {@link McpSyncServerCustomizer} bean that calls
 * {@link McpServerCustomizers#serverInfo(String, String, String, String)}.
 * The {@code andThen} chain keeps ordering deterministic.
 *
 * @author han
 * @time 2026/8/1 05:49
 */
public final class WireSchemaExporter {

    private WireSchemaExporter() {
    }

    /**
     * Synchronous variant: returns a {@link McpSyncServerCustomizer} that
     * applies the full listChanged / subscribe capability set.
     */
    public static McpSyncServerCustomizer syncAll() {
        return McpServerCustomizers.composeAll(
            McpServerCustomizers.syncListChangedAll()
        );
    }

    /**
     * Asynchronous variant: returns a {@link McpAsyncServerCustomizer} that
     * applies the full listChanged / subscribe capability set.
     */
    public static McpAsyncServerCustomizer asyncAll() {
        return McpServerCustomizers.composeAllAsync(
            McpServerCustomizers.asyncListChangedAll()
        );
    }

    /**
     * Combine wire-schema capabilities with a custom identity customizer.
     * Useful when the integrator wants to ship both listChanged AND a custom
     * server identity in one Bean:
     *
     * <pre>{@code
     *   &#64;Bean
     *   public McpSyncServerCustomizer wireSchemaExporter() {
     *       return WireSchemaExporter.syncAllAnd(
     *           McpServerCustomizers.serverInfo("my-mcp", "1.0.0", "My MCP", "Hello"));
     *   }
     * }</pre>
     */
    public static McpSyncServerCustomizer syncAllAnd(McpSyncServerCustomizer other) {
        return McpServerCustomizers.composeAll(
            McpServerCustomizers.syncListChangedAll(),
            other
        );
    }

    public static McpAsyncServerCustomizer asyncAllAnd(McpAsyncServerCustomizer other) {
        return McpServerCustomizers.composeAllAsync(
            McpServerCustomizers.asyncListChangedAll(),
            other
        );
    }

    /**
     * Return a {@code Map<String,Object>} suitable for {@code McpServerProperties.capabilities}
     * configuration when the user prefers YAML / properties over customizer
     * Beans. Shape mirrors {@link ServerCapabilitiesFactory#withListChangedAll()}
     * but expressed as a flat map that matches the spring.ai.mcp.server.capabilities.*
     * property tree.
     */
    public static Map<String, Object> capabilitiesAsMap() {
        Map<String, Object> caps = new LinkedHashMap<>();
        caps.put("tools", Map.of("listChanged", true));
        caps.put("resources", Map.of("subscribe", true, "listChanged", true));
        caps.put("prompts", Map.of("listChanged", true));
        return caps;
    }
}