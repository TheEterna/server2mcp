package com.ai.plug.core.spec.pagination;

import java.util.List;
import java.util.Map;

/**
 * One-shot helper that wires {@link McpPaging} (input) + {@link PageList}
 * (output) into a single result type for tool methods.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;Tool
 *   public PageList&lt;Row&gt; listRows(McpPaging paging) {
 *       int total = db.count();
 *       List&lt;Row&gt; slice = db.fetchSlice(paging.offset(), paging.size());
 *       return PageCache.wrap(slice, total, paging);
 *   }
 * }</pre>
 *
 * <p>{@link #wrap(List, int, McpPaging)} performs the trim + nextCursor
 * computation in one call: if the tool method returns the full slice (not
 * already trimmed), the helper trims to {@code paging.size()} and computes
 * the nextCursor. This lets tool authors be lazy and not have to remember
 * the paging contract.
 *
 * @author han
 * @time 2026/8/1 00:18
 */
public final class PageCache {

    private PageCache() {
    }

    /**
     * Build a {@link PageList} from a list of items + total count, with
     * nextCursor computed against the supplied {@link McpPaging}. If the
     * input list is larger than {@code paging.size()}, it's trimmed to fit.
     * Null paging defaults to {@link McpPaging#defaults()}.
     */
    public static <T> PageList<T> wrap(List<T> items, int totalItems, McpPaging paging) {
        McpPaging effective = paging == null ? McpPaging.defaults() : paging;
        int from = Math.min(effective.offset(), items.size());
        int to = Math.min(from + effective.size(), items.size());
        List<T> trimmed = items.subList(from, to);
        return new PageList<>(trimmed, totalItems);
    }

    /**
     * Variant that takes a raw Map of results + extracts cursor/pageSize
     * from request._meta — for tools that want to receive the full _meta
     * map directly (rather than the McpPaging convenience parameter).
     */
    public static <T> PageList<T> wrap(List<T> items, int totalItems, Map<String, Object> requestMeta) {
        McpPaging paging = McpPaging.fromCursor(
            requestMeta == null ? null : (String) requestMeta.get("cursor"),
            requestMeta == null ? null : ((Number) requestMeta.get("pageSize")).intValue());
        return wrap(items, totalItems, paging);
    }
}