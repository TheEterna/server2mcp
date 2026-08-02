package com.ai.plug.core.spec.change;

/**
 * Runnable wrapper for scheduled capability-change notification. Designed
 * to be invoked from any scheduler (Spring {@code @Scheduled}, custom
 * thread pool, test loop, etc).
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;Bean
 *   public McpToolChangeNotifier notifier(...) { ... }
 *
 *   &#64;Scheduled(fixedRate = 5_000)  // every 5 seconds
 *   public void pushChange() {
 *       CapabilityChangePushScheduler.wrap(notifier).run();
 *   }
 * }</pre>
 *
 * <p>Returns a {@link Runnable} so the scheduler can be wired to any
 * mechanism. Wrapping also enables future enhancements (e.g. batched
 * notifications, conditional gating) without changing call sites.
 */
public final class CapabilityChangePushScheduler {

    private CapabilityChangePushScheduler() {
    }

    /**
     * Build a {@link Runnable} that calls {@link McpToolChangeNotifier#diffAndNotify()}.
     */
    public static Runnable wrap(McpToolChangeNotifier notifier) {
        if (notifier == null) {
            throw new IllegalArgumentException("notifier is required");
        }
        return notifier::diffAndNotify;
    }

    /**
     * Build a {@link Runnable} that calls {@link McpToolChangeNotifier#notifyNow()}
     * — fire regardless of last-snapshot. Use when the user explicitly
     * wants to push a change (e.g. from a webhook handler).
     */
    public static Runnable wrapForce(McpToolChangeNotifier notifier) {
        if (notifier == null) {
            throw new IllegalArgumentException("notifier is required");
        }
        return notifier::notifyNow;
    }
}