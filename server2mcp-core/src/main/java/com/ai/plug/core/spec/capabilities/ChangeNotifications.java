package com.ai.plug.core.spec.capabilities;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * Server-side change notification helpers (protocol 2025-11-25 notifications/*
 * listChanged / updated).
 * <p>
 * MCP SDK 2.0 exposes {@code McpSyncServer.notifyToolsListChanged()} /
 * {@code notifyResourcesListChanged()} / {@code notifyResourcesUpdated(...)} /
 * {@code notifyPromptsListChanged()} (and async Mono-returning equivalents), but
 * these methods only fire when:
 * <ol>
 *   <li>Server capabilities declared {@code listChanged=true} at build time
 *       (Builder.tools(true) / Builder.resources(true, true) / Builder.prompts(true));</li>
 *   <li>Client opted-in to {@code tools.listChanged} / {@code resources.listChanged} /
 *       {@code resources.subscribe} / {@code prompts.listChanged} via its
 *       {@code ClientCapabilities}.</li>
 * </ol>
 * This util provides the second half: type-safe wrappers around the four SDK notify
 * methods so user code can fire them from anywhere a {@code McpSyncServer} or
 * {@code McpAsyncServer} is in scope. Capability declaration remains the
 * integrator's responsibility — see {@link ServerCapabilitiesFactory}.
 *
 * @author han
 * @time 2026/7/31 19:08
 */
public final class ChangeNotifications {

    private ChangeNotifications() {
    }

    // ---- sync ----

    public static void notifyToolsListChanged(McpSyncServer server) {
        server.notifyToolsListChanged();
    }

    public static void notifyResourcesListChanged(McpSyncServer server) {
        server.notifyResourcesListChanged();
    }

    public static void notifyResourcesUpdated(McpSyncServer server, String uri) {
        server.notifyResourcesUpdated(new McpSchema.ResourcesUpdatedNotification(uri));
    }

    public static void notifyPromptsListChanged(McpSyncServer server) {
        server.notifyPromptsListChanged();
    }

    // ---- async ----

    public static Mono<Void> notifyToolsListChanged(McpAsyncServer server) {
        return server.notifyToolsListChanged();
    }

    public static Mono<Void> notifyResourcesListChanged(McpAsyncServer server) {
        return server.notifyResourcesListChanged();
    }

    public static Mono<Void> notifyResourcesUpdated(McpAsyncServer server, String uri) {
        return server.notifyResourcesUpdated(new McpSchema.ResourcesUpdatedNotification(uri));
    }

    public static Mono<Void> notifyPromptsListChanged(McpAsyncServer server) {
        return server.notifyPromptsListChanged();
    }
}