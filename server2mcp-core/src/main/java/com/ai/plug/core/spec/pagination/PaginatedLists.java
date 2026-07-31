package com.ai.plug.core.spec.pagination;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * Pagination support for MCP list responses (tools/list, resources/list, prompts/list,
 * resource-templates/list).
 * <p>
 * MCP SDK 2.0 / Spring AI 2.0 do <b>not</b> expose list-handler hooks on
 * {@code SyncSpecification} / {@code AsyncSpecification} (verified by javap on the
 * shipped bytecode). Cursor / nextCursor fields exist on
 * {@code ListToolsResult} / {@code ListResourcesResult} / {@code ListPromptsResult}
 * but server-side slicing is the SDK's internal responsibility.
 * <p>
 * This util therefore targets the second-best position: <b>frame-level helpers that
 * user code can invoke when assembling their own MCP server</b> (e.g. when they
 * post-process this project's spec lists, or when they build an MCP server without
 * going through Spring AI auto-config). It is intentionally not wired into
 * {@code McpToolProvider} by default — pagination is a routing policy decision that
 * must remain the integrator's call.
 * <h2>用法</h2>
 * <pre>{@code
 *   // 1. Parse the cursor from a tools/list request
 *   int offset = PaginatedLists.parseOffset(request.cursor());
 *   int size = PaginatedLists.DEFAULT_PAGE_SIZE; // 50, or whatever you choose
 *
 *   // 2. Slice your tool list
 *   Page<Tool> page = PaginatedLists.slice(allTools, offset, size);
 *
 *   // 3. Build the ListToolsResult with nextCursor wired
 *   ListToolsResult result = PaginatedLists.toListToolsResult(
 *       page.items(), page.nextCursor());
 * }</pre>
 *
 * Cursor format: opaque non-negative decimal integer string. Server may swap to
 * any opaque scheme later — clients MUST treat the cursor as a black box.
 *
 * @author han
 * @time 2026/7/31 18:15
 */
public final class PaginatedLists {

    /** Default page size when the client doesn't negotiate one. */
    public static final int DEFAULT_PAGE_SIZE = 50;

    /** Maximum page size — clients requesting larger slices are clamped down. */
    public static final int MAX_PAGE_SIZE = 500;

    private PaginatedLists() {
    }

    /**
     * Parse an opaque cursor into a zero-based offset.
     *
     * @param cursor opaque cursor (e.g. from {@code PaginatedRequest.cursor()}); null / blank = 0
     * @return offset, never negative; round-trips with {@link #formatOffset(int)}
     * @throws IllegalArgumentException if the cursor is malformed
     */
    public static int parseOffset(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            int offset = Integer.parseInt(cursor);
            if (offset < 0) {
                throw new IllegalArgumentException("cursor must be non-negative, got: " + cursor);
            }
            return offset;
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid cursor format: " + cursor, ex);
        }
    }

    /**
     * Inverse of {@link #parseOffset(String)}. Returns null when offset is 0
     * (the natural starting position needs no cursor).
     */
    public static String formatOffset(int offset) {
        if (offset <= 0) {
            return null;
        }
        return Integer.toString(offset);
    }

    /**
     * Clamp the requested page size into [1, {@value #MAX_PAGE_SIZE}].
     * Treats {@code <= 0} as {@link #DEFAULT_PAGE_SIZE}.
     */
    public static int clampPageSize(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }

    /**
     * Slice a list with offset-based pagination.
     *
     * @param all     the full list (caller-supplied)
     * @param offset  zero-based offset (typically from {@link #parseOffset(String)})
     * @param size    page size (typically from {@link #clampPageSize(int)})
     * @return Page containing the slice + nextCursor (null when there are no more items)
     */
    public static <T> Page<T> slice(List<T> all, int offset, int size) {
        if (all == null || all.isEmpty() || offset >= all.size()) {
            return new Page<>(List.of(), null);
        }
        int safeSize = clampPageSize(size);
        int end = Math.min(offset + safeSize, all.size());
        List<T> items = List.copyOf(all.subList(offset, end));
        String nextCursor = end >= all.size() ? null : formatOffset(end);
        return new Page<>(items, nextCursor);
    }

    /** Build a {@link McpSchema.ListToolsResult} with optional nextCursor. */
    public static McpSchema.ListToolsResult toListToolsResult(List<McpSchema.Tool> tools, String nextCursor) {
        return new McpSchema.ListToolsResult(tools, nextCursor);
    }

    /** Build a {@link McpSchema.ListResourcesResult} with optional nextCursor. */
    public static McpSchema.ListResourcesResult toListResourcesResult(List<McpSchema.Resource> resources, String nextCursor) {
        return new McpSchema.ListResourcesResult(resources, nextCursor);
    }

    /** Build a {@link McpSchema.ListPromptsResult} with optional nextCursor. */
    public static McpSchema.ListPromptsResult toListPromptsResult(List<McpSchema.Prompt> prompts, String nextCursor) {
        return new McpSchema.ListPromptsResult(prompts, nextCursor);
    }

    /**
     * Immutable page result. {@code nextCursor} is null when the page exhausts the list.
     */
    public record Page<T>(List<T> items, String nextCursor) {

        public boolean hasMore() {
            return nextCursor != null;
        }
    }
}