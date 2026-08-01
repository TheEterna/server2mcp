package com.ai.plug.core.spec.change;

import com.ai.plug.core.annotation.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Bridges {@link com.ai.plug.core.context.tool.IToolContext} mutation events
 * to MCP server change notifications (protocol 2025-11-25 SEP-2567 listChanged).
 * <p>
 * Captures a snapshot of the context's tools map and exposes
 * {@link #diffAndNotify()} which fires the configured notifier Runnable /
 * Mono supplier when the snapshot differs from the previous one. Designed to
 * be invoked from a periodic background poll or wired into a Spring
 * ApplicationListener.
 *
 * <h2>典型用法</h2>
 * <pre>{@code
 *   // sync
 *   McpToolChangeNotifier notifier = McpToolChangeNotifier.forSync(
 *       toolContext, () -> syncServer.notifyToolsListChanged());
 *   // async
 *   McpToolChangeNotifier notifier = McpToolChangeNotifier.forAsync(
 *       toolContext, () -> asyncServer.notifyToolsListChanged().subscribe());
 *
 *   scheduler.scheduleAtFixedRate(notifier::diffAndNotify, 0, 1, TimeUnit.SECONDS);
 * }</pre>
 *
 * <p>设计取舍：notifier 用 pull-poll + 抽象 callback 而非具体 MCP server 类型，
 * 避免 hard dep on SDK final class 让单元测试无需 Mockito；调用方在自己组装
 * lambda 时把 SDK 类型包进 callback 即可。
 *
 * @author han
 * @time 2026/8/1 05:49
 */
public class McpToolChangeNotifier {

    private static final Logger log = LoggerFactory.getLogger(McpToolChangeNotifier.class);

    private final Object toolContext; // IToolContext — typed as Object to avoid dep cycle
    @Nullable
    private final Runnable syncNotifier;
    @Nullable
    private final java.util.function.Supplier<Mono<Void>> asyncNotifier;
    private volatile int lastSize = -1;
    private volatile int lastHash = 0;

    public McpToolChangeNotifier(Object toolContext, @Nullable Runnable syncNotifier,
                                   @Nullable java.util.function.Supplier<Mono<Void>> asyncNotifier) {
        if (toolContext == null) {
            throw new IllegalArgumentException("toolContext is required");
        }
        if (syncNotifier == null && asyncNotifier == null) {
            throw new IllegalArgumentException("at least one of syncNotifier / asyncNotifier must be non-null");
        }
        this.toolContext = toolContext;
        this.syncNotifier = syncNotifier;
        this.asyncNotifier = asyncNotifier;
    }

    /** Static factory for synchronous server notification. */
    public static McpToolChangeNotifier forSync(Object toolContext, Runnable syncNotifier) {
        return new McpToolChangeNotifier(toolContext, syncNotifier, null);
    }

    /** Static factory for asynchronous server notification. */
    public static McpToolChangeNotifier forAsync(Object toolContext,
                                                  java.util.function.Supplier<Mono<Void>> asyncNotifier) {
        return new McpToolChangeNotifier(toolContext, null, asyncNotifier);
    }

    /**
     * Compare current tool set against the last-seen snapshot; if size or
     * hash changed, fire the configured notifier(s). Safe to call from any
     * thread.
     */
    public void diffAndNotify() {
        int currentSize;
        int currentHash;
        try {
            Map<String, Object> rawTools = invokeGetRawTools();
            currentSize = rawTools.size();
            currentHash = rawTools.hashCode();
        }
        catch (Exception ex) {
            log.warn("diffAndNotify failed to read tool context: {}", ex.getMessage());
            return;
        }
        if (currentSize == lastSize && currentHash == lastHash) {
            return;
        }
        lastSize = currentSize;
        lastHash = currentHash;
        try {
            if (syncNotifier != null) {
                syncNotifier.run();
            }
            if (asyncNotifier != null) {
                asyncNotifier.get().subscribe();
            }
            log.debug("Tools list changed (size={}); notified {} channel(s)",
                currentSize, (syncNotifier != null ? 1 : 0) + (asyncNotifier != null ? 1 : 0));
        }
        catch (Exception ex) {
            log.warn("notify failed: {}", ex.getMessage());
        }
    }

    /**
     * Reflectively invoke {@code IToolContext.getRawTools()} to avoid a hard
     * dependency on the interface from this util (the interface lives in
     * {@code core.context.tool} and the change package stays generic).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeGetRawTools() throws Exception {
        try {
            var m = toolContext.getClass().getMethod("getRawTools");
            Object result = m.invoke(toolContext);
            return (Map<String, Object>) result;
        }
        catch (NoSuchMethodException ex) {
            throw new IllegalStateException(
                "toolContext " + toolContext.getClass().getName() + " has no getRawTools() method", ex);
        }
    }

    /** Reset internal snapshot — useful for forcing a re-fire after manual edits. */
    public void resetSnapshot() {
        this.lastSize = -1;
        this.lastHash = 0;
    }

    /**
     * Convenience alias for {@link #diffAndNotify()} for callers that want a
     * less implementation-y name. Suitable for binding as a Spring
     * {@code ApplicationListener<ContextRefreshedEvent>} or for explicit
     * programmatic triggering after manual controller edits.
     */
    public void notifyNow() {
        diffAndNotify();
    }

    /**
     * Static helper for callers that want per-tool filtering on listChanged
     * notification: returns whether the {@link McpTool#listChanged()} flag
     * indicates a dynamic tool (true by default). Use this in your own
     * scheduler code alongside {@link #forSync(Object, Runnable)}:
     *
     * <pre>{@code
     *   Runnable notifier = () -> {
     *       if (McpToolChangeNotifier.shouldNotifyAnyOf(ctx, method -> method.isAnnotationPresent(McpTool.class)
     *               &#38;&#38; method.getAnnotation(McpTool.class).listChanged())) {
     *           syncServer.notifyToolsListChanged();
     *       }
     *   };
     * }</pre>
     */
    public static boolean isListChanged(@org.jspecify.annotations.Nullable McpTool ann) {
        return ann == null || ann.listChanged();
    }

    /**
     * Spring ApplicationListener entry point — fire one notification when the
     * application context is ready. Use by registering this class as a Spring
     * bean and Spring will call this method automatically.
     *
     * <pre>{@code
     *   &#64;Bean
     *   public McpToolChangeNotifier toolChangeNotifier(IToolContext ctx, McpSyncServer server) {
     *       return McpToolChangeNotifier.forSync(ctx, () -> server.notifyToolsListChanged());
     *   }
     *   // spring auto-registers ContextRefreshedEvent listeners
     * }</pre>
     *
     * The first notification carries the initial snapshot (lastSize=-1 →
     * fires once even if no changes); subsequent applications are no-ops
     * unless something changed.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        notifyNow();
    }
}