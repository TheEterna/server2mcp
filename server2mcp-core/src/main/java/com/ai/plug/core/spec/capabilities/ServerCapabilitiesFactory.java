package com.ai.plug.core.spec.capabilities;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * Builder for {@link McpSchema.ServerCapabilities} with {@code listChanged} flags
 * pre-set to {@code true} (matching protocol 2025-11-25 expectations that
 * clients should be informed of dynamic changes).
 * <p>
 * SDK 2.0's {@code McpServer$SyncSpecification} / {@code AsyncSpecification} accept
 * a pre-built {@code ServerCapabilities} via {@code .capabilities(...)}, but the
 * default builder starts with all capability flags null — meaning the SDK will
 * not actually fire any change notifications even if you call them, unless you
 * opt in explicitly.
 * <p>
 * Use this factory when assembling your MCP server:
 * <pre>{@code
 *   McpServer.SyncSpecification spec = ...
 *       .serverInfo("name", "1.0")
 *       .capabilities(ServerCapabilitiesFactory.withListChangedAll())
 *       .tools(tools)
 *       .build();
 * }</pre>
 *
 * @author han
 * @time 2026/7/31 19:08
 */
public final class ServerCapabilitiesFactory {

    private ServerCapabilitiesFactory() {
    }

    /**
     * Capabilities with tools/prompts/resources all set to {@code listChanged=true},
     * plus {@code subscribe=true} on resources. Matches the strongest
     * {@code ServerCapabilities} a server can declare.
     */
    public static McpSchema.ServerCapabilities withListChangedAll() {
        return McpSchema.ServerCapabilities.builder()
                .tools(true)
                .resources(true, true)
                .prompts(true)
                .build();
    }

    /**
     * Capabilities with only {@code tools.listChanged=true}; resources / prompts
     * remain at SDK default (null = no declaration).
     */
    public static McpSchema.ServerCapabilities withToolsListChanged() {
        return McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build();
    }

    /**
     * Capabilities with only {@code resources.listChanged=true} and
     * {@code resources.subscribe=true}; tools / prompts stay at default.
     */
    public static McpSchema.ServerCapabilities withResourcesListChanged() {
        return McpSchema.ServerCapabilities.builder()
                .resources(true, true)
                .build();
    }

    /**
     * Capabilities with only {@code prompts.listChanged=true}; tools / resources
     * stay at default.
     */
    public static McpSchema.ServerCapabilities withPromptsListChanged() {
        return McpSchema.ServerCapabilities.builder()
                .prompts(true)
                .build();
    }

    /**
     * Escape hatch: wrap your own map of additional {@code experimental}
     * capabilities (e.g. {@code Tasks} extensions) while keeping the standard
     * listChanged flags on. Pass null to omit.
     */
    public static McpSchema.ServerCapabilities withExperimental(Map<String, Object> experimental) {
        return McpSchema.ServerCapabilities.builder()
                .tools(true)
                .resources(true, true)
                .prompts(true)
                .experimental(experimental)
                .build();
    }
}