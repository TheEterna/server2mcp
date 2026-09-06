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

import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * Default {@link TenantResolver} — pulls the tenant ID from the
 * {@code X-Mcp-Tenant} HTTP header.
 *
 * <p>Falls back to {@link TenantContext#EMPTY} when:
 * <ul>
 *   <li>no servlet request is bound to the current thread (e.g. a
 *       scheduled task running outside any HTTP scope), or</li>
 *   <li>the header is missing or blank.</li>
 * </ul>
 *
 * <p>Reactive request scope is not handled here — for WebFlux
 * deployments, register a custom {@link TenantResolver} that reads
 * from Reactor {@code Context} (the WebFlux starter exposes a
 * reactive-aware extension point).
 */
public class HeaderTenantResolver implements TenantResolver {

    public static final String DEFAULT_HEADER = "X-Mcp-Tenant";

    private final String headerName;

    public HeaderTenantResolver() {
        this(DEFAULT_HEADER);
    }

    public HeaderTenantResolver(String headerName) {
        if (headerName == null || headerName.isBlank()) {
            throw new IllegalArgumentException("headerName is required");
        }
        this.headerName = headerName;
    }

    @Override
    public String resolve() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return TenantContext.EMPTY;
        }
        HttpServletRequest request = servletAttrs.getRequest();
        if (request == null) {
            return TenantContext.EMPTY;
        }
        List<String> values = Collections.list(request.getHeaders(headerName));
        if (values.isEmpty()) {
            return TenantContext.EMPTY;
        }
        // Multi-value is unusual; first wins.
        String first = values.get(0);
        return first == null || first.isBlank() ? TenantContext.EMPTY : first.trim();
    }

    /** Convenience for callers that already have a {@link HttpHeaders}. */
    public String resolve(HttpHeaders headers) {
        if (headers == null) {
            return TenantContext.EMPTY;
        }
        List<String> values = headers.get(headerName);
        if (values == null || values.isEmpty()) {
            return TenantContext.EMPTY;
        }
        String first = values.get(0);
        return first == null || first.isBlank() ? TenantContext.EMPTY : first.trim();
    }
}
