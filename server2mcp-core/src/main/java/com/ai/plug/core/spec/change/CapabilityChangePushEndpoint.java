package com.ai.plug.core.spec.change;

/**
 * HTTP endpoint contract for triggering capability-change notifications
 * (e.g. {@code POST /admin/mcp/push-change}). Avoids Spring Web dependency in
 * the core module — downstream modules implement actual HTTP binding.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var endpoint = new CapabilityChangePushEndpoint(notifier);
 *   endpoint.handlePush();  // fire change notification
 * </pre>
 *
 * <p>Designed for ops scenarios:
 * <ul>
 *   <li>Manual: admin triggers push after controller add/remove
 *   <li>Webhook: external system pings after a deploy
 *   <li>Forced: bypass diffAndNotify() and always fire (use notifyNow())
 * </ul>
 */
public class CapabilityChangePushEndpoint {

    private final McpToolChangeNotifier notifier;

    public CapabilityChangePushEndpoint(McpToolChangeNotifier notifier) {
        if (notifier == null) {
            throw new IllegalArgumentException("notifier is required");
        }
        this.notifier = notifier;
    }

    /**
     * Trigger a change notification. Default mode: calls diffAndNotify()
     * which only fires when something changed. Returns the number of
     * notifications fired (0 or 1).
     */
    public int handlePush() {
        int before = currentSize();
        notifier.diffAndNotify();
        int after = currentSize();
        return after - before;
    }

    /**
     * Force a change notification regardless of last snapshot. Returns 1.
     */
    public int handleForcePush() {
        notifier.notifyNow();
        return 1;
    }

    /**
     * Best-effort peek at the notifier's internal size — reflection on the
     * store field. Returns -1 if reflection fails (introspection only,
     * never throws).
     */
    private int currentSize() {
        try {
            var f = McpToolChangeNotifier.class.getDeclaredField("lastSize");
            f.setAccessible(true);
            return f.getInt(notifier);
        }
        catch (Exception ex) {
            return -1;
        }
    }
}