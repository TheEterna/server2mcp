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

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the default {@link HeaderTenantResolver}. Covers the
 * three resolution paths: header present, header missing, and
 * no servlet request bound at all (e.g. background thread).
 */
class HeaderTenantResolverTest {

    @org.junit.jupiter.api.AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolve_returnsHeaderValueWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Mcp-Tenant", "acme");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("acme", new HeaderTenantResolver().resolve());
    }

    @Test
    void resolve_returnsEmptyWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals(TenantContext.EMPTY, new HeaderTenantResolver().resolve());
    }

    @Test
    void resolve_returnsEmptyWhenNoServletRequest() {
        // No RequestContextHolder set → background thread.
        assertEquals(TenantContext.EMPTY, new HeaderTenantResolver().resolve());
    }

    @Test
    void resolve_trimsWhitespace() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Mcp-Tenant", "  acme  ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("acme", new HeaderTenantResolver().resolve());
    }

    @Test
    void resolve_picksFirstValueOnMultiHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Mcp-Tenant", "first");
        request.addHeader("X-Mcp-Tenant", "second");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("first", new HeaderTenantResolver().resolve());
    }

    @Test
    void constructor_rejectsBlankHeaderName() {
        assertThrows(IllegalArgumentException.class, () -> new HeaderTenantResolver(""));
        assertThrows(IllegalArgumentException.class, () -> new HeaderTenantResolver(null));
    }

    @Test
    void resolve_fromHttpHeaders_handlesNullAndEmpty() {
        HeaderTenantResolver resolver = new HeaderTenantResolver();
        assertEquals(TenantContext.EMPTY, resolver.resolve((HttpHeaders) null));

        HttpHeaders empty = new HttpHeaders();
        assertEquals(TenantContext.EMPTY, resolver.resolve(empty));

        HttpHeaders present = new HttpHeaders();
        present.add("X-Mcp-Tenant", "acme");
        assertEquals("acme", resolver.resolve(present));
    }
}
