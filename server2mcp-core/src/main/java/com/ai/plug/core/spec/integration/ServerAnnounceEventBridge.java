package com.ai.plug.core.spec.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import reactor.util.annotation.Nullable;

import java.util.function.Consumer;

/**
 * Spring event bridge for {@link ServerAnnounce} — emits the announce
 * payload to a sink when the application context is ready.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;Bean
 *   public ServerAnnounceEventBridge announceBridge(McpSyncServer server) {
 *       ServerAnnounce ann = ServerAnnounce.builder()
 *           .info(ServerInfoFactory.create("my-mcp", "1.0"))
 *           .capabilities(ServerCapabilitiesFactory.withListChangedAll())
 *           .build();
 *       // Sink: any consumer (logger, SSE broadcaster, custom transport hook, ...)
 *       Consumer&lt;ServerAnnounce&gt; sink = a -&gt;
 *           server.notifyResourcesUpdated(...);  // example placeholder
 *       return new ServerAnnounceEventBridge(ann, sink);
 *   }
 * }</pre>
 *
 * <p>The bridge is independent of the MCP transport — it only emits the
 * announcement to a user-supplied consumer, who decides how to surface
 * it (SSE / WebSocket / logs / etc).
 *
 * @author han
 * @time 2026/8/1 00:18
 */
public class ServerAnnounceEventBridge {

    private static final Logger log = LoggerFactory.getLogger(ServerAnnounceEventBridge.class);

    private final ServerAnnounce announce;
    @Nullable
    private final Consumer<ServerAnnounce> sink;

    public ServerAnnounceEventBridge(ServerAnnounce announce, @Nullable Consumer<ServerAnnounce> sink) {
        if (announce == null) {
            throw new IllegalArgumentException("announce is required");
        }
        this.announce = announce;
        this.sink = sink;
    }

    /**
     * Spring ApplicationListener — fires when the application context is
     * fully refreshed. Emits the announcement to the configured sink.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        emit();
    }

    /**
     * Programmatic trigger — useful for tests and for integrators that
     * want to fire the announcement at a different lifecycle moment.
     */
    public void emit() {
        if (sink == null) {
            log.debug("No sink configured; announcement dropped (server={}, version={})",
                announce.serverInfo().name(), announce.serverInfo().version());
            return;
        }
        try {
            sink.accept(announce);
        }
        catch (Exception ex) {
            log.warn("Announcement sink threw: {}", ex.getMessage());
        }
    }

    /** @return the announcement this bridge would emit. */
    public ServerAnnounce announce() {
        return announce;
    }
}