package com.ai.plug.core.spec.resulttype;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.function.Supplier;

/**
 * Spring ApplicationListener — runs {@link WireSchemaValidator} on the
 * current {@link McpSchema.ServerCapabilities} when the application context
 * is ready. In dev mode this surfaces missing/malformed wire-layer fields
 * early (at startup) rather than at the first {@code tools/list} RPC.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;Bean
 *   public WireSchemaValidationListener validator(McpSyncServer server) {
 *       return new WireSchemaValidationListener(() -&gt; server.spec().capabilities());
 *   }
 * }</pre>
 *
 * <p>Reports issues as {@code WARN} log lines. By default does not
 * throw — startup continues even with schema problems. Use
 * {@link #WireSchemaValidationListener(Supplier, boolean)} with
 * {@code strict=true} to abort startup on issues.
 */
public class WireSchemaValidationListener {

    private static final Logger log = LoggerFactory.getLogger(WireSchemaValidationListener.class);

    private final java.util.function.Supplier<McpSchema.ServerCapabilities> source;
    private final boolean strict;

    public WireSchemaValidationListener(
            java.util.function.Supplier<McpSchema.ServerCapabilities> source) {
        this(source, false);
    }

    public WireSchemaValidationListener(
            java.util.function.Supplier<McpSchema.ServerCapabilities> source, boolean strict) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        this.source = source;
        this.strict = strict;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        McpSchema.ServerCapabilities caps;
        try {
            caps = source.get();
        }
        catch (Exception ex) {
            log.warn("Capabilities source threw: {}", ex.getMessage());
            return;
        }
        WireSchemaValidator.Report report = WireSchemaValidator.validateMeta(
            caps.experimental(), "ServerCapabilities");
        if (!report.isOk()) {
            if (strict) {
                throw new IllegalStateException(
                    "WireSchema validation failed:\n" + report);
            }
            log.warn("WireSchema validation found issues:\n{}", report);
        }
        else {
            log.info("WireSchema validation passed");
        }
    }
}