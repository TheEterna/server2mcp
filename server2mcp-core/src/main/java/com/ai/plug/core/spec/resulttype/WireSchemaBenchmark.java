package com.ai.plug.core.spec.resulttype;

import com.ai.plug.core.spec.cacheable.CacheHints;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Microbenchmark for {@link McpResultWriter#writeCallToolResultFromMeta}.
 * Measures throughput + latency for the framework's most common wire-layer
 * serialization path.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   WireSchemaBenchmark.Result r = WireSchemaBenchmark.run(10_000);
 *   log.info("throughput={} ops/s, mean={} ns/op", r.opsPerSec(), r.meanNs());
 * }</pre>
 *
 * <p>Not a JMH-grade benchmark — intended for rough sanity checks during
 * development. The framework's serialization path is dominated by Jackson 3's
 * own overhead; the framework's contribution is the meta-to-JSON field
 * mutation (~few microseconds per call).
 */
public final class WireSchemaBenchmark {

    private static final Logger log = LoggerFactory.getLogger(WireSchemaBenchmark.class);

    private WireSchemaBenchmark() {
    }

    /**
     * Run the benchmark with the given iteration count and report.
     */
    public static Result run(int iterations) throws java.io.IOException {
        McpSchema.CallToolResult sample = buildSample();
        // Warmup
        for (int i = 0; i < 1000; i++) {
            McpResultWriter.writeCallToolResultFromMeta(sample);
        }
        long startNs = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            McpResultWriter.writeCallToolResultFromMeta(sample);
        }
        long elapsedNs = System.nanoTime() - startNs;
        double meanNs = (double) elapsedNs / iterations;
        double opsPerSec = (double) iterations / ((double) elapsedNs / 1_000_000_000.0);

        Result result = new Result(iterations, meanNs, opsPerSec, elapsedNs);
        log.info("WireSchemaBenchmark: {}", result);
        return result;
    }

    /**
     * Build a realistic sample CallToolResult for benchmarking.
     */
    private static McpSchema.CallToolResult buildSample() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("resultType", "complete");
        meta.put("ttlMs", 60_000L);
        meta.put("cacheScope", "private");
        meta.put("cacheWrapperKey", "_cacheable");
        return McpSchema.CallToolResult.builder()
            .addTextContent("hello world")
            .isError(false)
            .meta(meta)
            .build();
    }

    /** Benchmark result. */
    public record Result(int iterations, double meanNs, double opsPerSec, long totalNs) {
        public Duration totalDuration() {
            return Duration.ofNanos(totalNs);
        }
        @Override
        public String toString() {
            return "Result[iters=" + iterations + ", mean=" + String.format("%.1f", meanNs)
                + " ns/op, throughput=" + String.format("%.0f", opsPerSec) + " ops/s, total="
                + totalDuration().toMillis() + " ms]";
        }
    }

    /**
     * Convenience overload using {@link CacheHints} for the cache metadata.
     * Demonstrates the canonical "all four wire fields" payload.
     */
    public static Result runFull(int iterations) throws java.io.IOException {
        McpSchema.CallToolResult sample = McpSchema.CallToolResult.builder()
            .addTextContent("full payload")
            .isError(false)
            .meta(new HashMap<>(Map.of(
                "resultType", "complete",
                "ttlMs", 60_000L,
                "cacheScope", "public",
                "cacheWrapperKey", "_cacheable")))
            .build();
        long startNs = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            McpResultWriter.writeCallToolResultFromMeta(sample);
        }
        long elapsedNs = System.nanoTime() - startNs;
        double meanNs = (double) elapsedNs / iterations;
        return new Result(iterations, meanNs,
            (double) iterations / ((double) elapsedNs / 1_000_000_000.0), elapsedNs);
    }
}