/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.tenant;

/**
 * Per-request tenant identity. The framework keeps the active tenant
 * ID in a {@link ThreadLocal} so that downstream code (tool callbacks,
 * converters, filters) can answer the question
 * "is this tool available to the caller?" without re-parsing the
 * transport envelope.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>HTTP entry-points (the framework's controllers + any user
 *       {@code @RestController}) should call {@link #set(String)} at
 *       the top of each request and {@link #clear()} in a
 *       {@code finally} block;</li>
 *   <li>The MCP JSON-RPC entry-point
 *       ({@code com.ai.plug.starter.webmvc.JsonRpcController} and its
 *       WebFlux counterpart) does this automatically — users of the
 *       starter do not need to wire it themselves;</li>
 *   <li>Stale values are impossible: {@link #set} overwrites any prior
 *       value, and {@link #clear} resets to {@link #EMPTY}.</li>
 * </ul>
 *
 * <h2>Why a {@code ThreadLocal}?</h2>
 * WebMVC runs each request on its own thread → ThreadLocal scope
 * matches the request. WebFlux runs on Netty event-loop threads that
 * are reused across requests, so a plain ThreadLocal would leak
 * tenant IDs between requests. The reactive counterpart uses the
 * Reactor {@code Context} propagation — see
 * {@code com.ai.plug.starter.webflux.JsonRpcController} for the
 * bridge.
 *
 * @author han
 * @time 2026/8/3
 */
public final class TenantContext {

    /** Sentinel value: "no tenant resolved" — every tool is permitted. */
    public static final String EMPTY = "";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    /** Returns the active tenant ID, or {@link #EMPTY} if none is set. */
    public static String get() {
        String value = CURRENT.get();
        return value == null ? EMPTY : value;
    }

    /** Set the active tenant ID for the current thread. Replaces any
     *  prior value. */
    public static void set(String tenantId) {
        CURRENT.set(tenantId == null ? EMPTY : tenantId);
    }

    /** Reset the active tenant ID. Must be called in a {@code finally}
     *  block to prevent thread-pool leaks. */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Run {@code action} with the given tenant ID active, restoring the
     * previous value afterward. Useful for background tasks spawned
     * inside a request.
     */
    public static void runWith(String tenantId, Runnable action) {
        String previous = CURRENT.get();
        try {
            set(tenantId);
            action.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
