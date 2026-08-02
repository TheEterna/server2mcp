package com.ai.plug.core.spec.dedup;

import com.ai.plug.core.annotation.McpTool;
import reactor.util.annotation.Nullable;

/**
 * Per-tool retry policy derived from {@link McpTool} annotation flags.
 *
 * <h2>规则</h2>
 * <ul>
 *   <li>{@code @McpTool(idempotentHint=true)} → max 1 retry on transient
 *       failures (idempotent operations are safe to repeat). Default.</li>
 *   <li>{@code @McpTool(idempotentHint=false)} → no retry (non-idempotent
 *       side effects may compound). This is the safe default — the framework
 *       will NOT auto-retry unless the tool is explicitly marked idempotent.</li>
 *   <li>{@code @McpTool(destructiveHint=true)} → no retry (destructive
 *       operations need careful consideration; the framework never retries
 *       them automatically).</li>
 * </ul>
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   if (RetryPolicy.shouldAutoRetry(toolAnnotation)) {
 *       // Run the tool again, up to RetryPolicy.maxRetries(toolAnnotation) times
 *   }
 * }</pre>
 *
 * @author han
 * @time 2026/8/1 05:49
 */
public final class RetryPolicy {

    /** Default retries for idempotent, non-destructive tools. */
    public static final int DEFAULT_IDEMPOTENT_MAX_RETRIES = 1;

    /** Conservative default for non-idempotent tools (no auto-retry). */
    public static final int DEFAULT_MAX_RETRIES = 0;

    private RetryPolicy() {
    }

    /**
     * @return true if the framework should automatically retry a failed
     *         invocation of this tool. Requires {@code idempotentHint=true}
     *         and {@code destructiveHint=false} (or default).
     */
    public static boolean shouldAutoRetry(@Nullable McpTool ann) {
        if (ann == null) {
            return false;
        }
        if (ann.destructiveHint()) {
            return false;
        }
        return ann.idempotentHint();
    }

    /**
     * @return the maximum number of retry attempts for the tool. 0 means
     *         no retries (single attempt).
     */
    public static int maxRetries(@Nullable McpTool ann) {
        if (!shouldAutoRetry(ann)) {
            return 0;
        }
        return DEFAULT_IDEMPOTENT_MAX_RETRIES;
    }
}