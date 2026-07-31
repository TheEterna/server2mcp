package com.ai.plug.core.spec.pagination;

import org.jspecify.annotations.Nullable;

/**
 * Paging context injected into tool methods as a parameter of type
 * {@code McpPaging} — lets a tool method enumerate over a page-bounded slice
 * of a larger dataset without the caller (typically an LLM agent) needing to
 * track cursor state.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;Tool
 *   public Page&lt;Row&gt; listUsers(McpPaging paging) {
 *       return dataSource.fetchSlice(paging.offset(), paging.size());
 *   }
 * }</pre>
 *
 * <p>The converter / framework fills the value from the
 * {@code tools/call} request's {@code _meta.paging} (cursor + size), or
 * from default values when the caller omits paging params.
 *
 * <h2>与 {@link PaginatedLists} 的关系</h2>
 * {@link McpPaging} is the *input* — what the framework passes into the tool.
 * {@link PaginatedLists} provides *output-side* helpers (slicing lists into
 * {@code List*Result}s with {@code nextCursor}). Together they form a
 * round-trip paging flow:
 *
 * <pre>{@code
 *   client -> (cursor=N) -> framework injects McpPaging(offset=N, size=50)
 *                          -> tool returns Page&lt;Row&gt;
 *                          -> framework wraps in ListToolsResult with nextCursor
 *   client -> (cursor=next) -> ... repeats
 * }</pre>
 *
 * @author han
 * @time 2026/8/1 05:49
 */
public record McpPaging(int offset, int size) {

    /** Default page size used when caller doesn't specify one. */
    public static final int DEFAULT_PAGE_SIZE = PaginatedLists.DEFAULT_PAGE_SIZE;
    /** Maximum page size enforced regardless of caller request. */
    public static final int MAX_PAGE_SIZE = PaginatedLists.MAX_PAGE_SIZE;

    public McpPaging {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0, got: " + offset);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0, got: " + size);
        }
        if (size > MAX_PAGE_SIZE) {
            // Silently clamp to MAX rather than throw — caller likely sent a
            // noisy request; clamping preserves the paging contract.
            size = MAX_PAGE_SIZE;
        }
    }

    /**
     * Default paging — offset 0, default size. Used when caller doesn't
     * pass any paging params.
     */
    public static McpPaging defaults() {
        return new McpPaging(0, DEFAULT_PAGE_SIZE);
    }

    /**
     * Build from raw caller values (typically from request._meta.paging),
     * with safe defaults for missing fields.
     */
    public static McpPaging of(Integer offsetOrNull, Integer sizeOrNull) {
        int offset = offsetOrNull == null || offsetOrNull < 0 ? 0 : offsetOrNull;
        int size = sizeOrNull == null || sizeOrNull <= 0 ? DEFAULT_PAGE_SIZE
                : Math.min(sizeOrNull, MAX_PAGE_SIZE);
        return new McpPaging(offset, size);
    }

    /**
     * Build from a cursor string (parsed via {@link PaginatedLists#parseOffset(String)})
     * and a raw size. Cursor may be null / blank (= offset 0).
     */
    public static McpPaging fromCursor(String cursor, Integer sizeOrNull) {
        int offset = cursor == null || cursor.isBlank() ? 0 : PaginatedLists.parseOffset(cursor);
        return of(offset, sizeOrNull);
    }

    /**
     * Compute the {@code nextCursor} for this page given the total number
     * of items available. Returns null when this page already covers
     * everything (i.e. there are no more items to fetch).
     */
    @Nullable
    public String nextCursor(int totalItems) {
        int nextOffset = offset + size;
        if (nextOffset >= totalItems) {
            return null;
        }
        return PaginatedLists.formatOffset(nextOffset);
    }
}