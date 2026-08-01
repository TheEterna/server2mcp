package com.ai.plug.core.spec.capabilities;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Periodic capabilities audit reporter. Snapshots the current
 * {@link McpSchema.ServerCapabilities} via {@link CapabilitySnapshot} and
 * logs a diff against the previous snapshot — operators get a clear audit
 * trail of capability changes (e.g. when a developer adds a new tool).
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;Bean
 *   public CapabilityAuditReporter reporter(McpServer server) {
 *       return new CapabilityAuditReporter(() -> server.spec().capabilities());
 *   }
 *
 *   &#64;Scheduled(fixedRate = 60_000)
 *   public void audit() {
 *       reporter.snapshotAndReport();
 *   }
 * }</pre>
 *
 * <p>Reporting goes to a pluggable sink (default: SLF4J INFO). Use the
 * constructor variant with a {@link java.util.function.Consumer} to forward
 * to Micrometer, a file appender, or any metrics backend.
 */
public class CapabilityAuditReporter {

    private static final Logger log = LoggerFactory.getLogger(CapabilityAuditReporter.class);

    private final java.util.function.Supplier<McpSchema.ServerCapabilities> capabilitiesSource;
    private final java.util.function.Consumer<String> sink;
    private final AtomicReference<CapabilitySnapshot> last = new AtomicReference<>();

    public CapabilityAuditReporter(java.util.function.Supplier<McpSchema.ServerCapabilities> source) {
        this(source, log::info);
    }

    public CapabilityAuditReporter(java.util.function.Supplier<McpSchema.ServerCapabilities> source,
                                   java.util.function.Consumer<String> sink) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        if (sink == null) {
            throw new IllegalArgumentException("sink is required");
        }
        this.capabilitiesSource = source;
        this.sink = sink;
    }

    /**
     * Take a fresh snapshot, compute the diff against the last snapshot, and
     * emit a report line through the sink. Returns the diff string (empty
     * on no change).
     */
    public String snapshotAndReport() {
        McpSchema.ServerCapabilities caps = capabilitiesSource.get();
        CapabilitySnapshot current = CapabilitySnapshot.from(caps);
        CapabilitySnapshot previous = last.get();
        String diff = previous == null ? "" : previous.diff(current);
        last.set(current);

        if (diff.isEmpty()) {
            sink.accept("CapabilityAuditReporter: no changes (snapshot=" + current.flags() + ")");
        }
        else {
            sink.accept("CapabilityAuditReporter: capability change detected:\n" + diff);
        }
        return diff;
    }

    /**
     * Force the next snapshot to be treated as a baseline — useful for
     * resetting state when the capabilities source itself has been replaced.
     */
    public void resetBaseline() {
        last.set(null);
    }

    /**
     * Current snapshot of capabilities (last taken). For inspection.
     */
    public Map<String, Boolean> currentFlags() {
        CapabilitySnapshot s = last.get();
        return s == null ? Map.of() : s.flags();
    }
}