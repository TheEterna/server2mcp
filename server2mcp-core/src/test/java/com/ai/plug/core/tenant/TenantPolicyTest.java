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

import com.ai.plug.core.annotation.McpTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decision-table tests for {@link TenantPolicy}. The policy is the
 * single source of truth for who-can-call-what, so we cover every
 * priority combination in the truth table.
 */
class TenantPolicyTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void nullAnnotation_isAlwaysVisible() {
        assertTrue(TenantPolicy.isVisible(null, "any-tenant"));
        assertTrue(TenantPolicy.isVisible(null, TenantContext.EMPTY));
    }

    @Test
    void denyAll_isNeverVisible() {
        McpTool a = annotation(true, new String[]{});
        assertFalse(TenantPolicy.isVisible(a, "any"));
        assertFalse(TenantPolicy.isVisible(a, "admin"));
        assertFalse(TenantPolicy.isVisible(a, TenantContext.EMPTY));
    }

    @Test
    void emptyTenantsList_isVisibleToEveryone() {
        McpTool a = annotation(false, new String[]{});
        assertTrue(TenantPolicy.isVisible(a, "alice"));
        assertTrue(TenantPolicy.isVisible(a, TenantContext.EMPTY));
    }

    @Test
    void allowList_matchesExactString() {
        McpTool a = annotation(false, new String[]{"acme", "globex"});
        assertTrue(TenantPolicy.isVisible(a, "acme"));
        assertTrue(TenantPolicy.isVisible(a, "globex"));
        assertFalse(TenantPolicy.isVisible(a, "other"));
        assertFalse(TenantPolicy.isVisible(a, "ACME"));  // case-sensitive
    }

    @Test
    void allowList_rejectsEmptyTenant() {
        McpTool a = annotation(false, new String[]{"acme"});
        assertFalse(TenantPolicy.isVisible(a, TenantContext.EMPTY));
        assertFalse(TenantPolicy.isVisible(a, null));
    }

    @Test
    void requireAccess_throwsForForbiddenTenant() {
        McpTool a = annotation(false, new String[]{"acme"});
        assertThrows(McpAccessDeniedException.class,
            () -> TenantPolicy.requireAccess(a, "secretTool", "globex"));
    }

    @Test
    void requireAccess_passesForAllowedTenant() {
        McpTool a = annotation(false, new String[]{"acme"});
        assertDoesNotThrow(() -> TenantPolicy.requireAccess(a, "publicTool", "acme"));
    }

    @Test
    void requireAccess_denyAllAlwaysThrows() {
        McpTool a = annotation(true, new String[]{});
        McpAccessDeniedException ex = assertThrows(McpAccessDeniedException.class,
            () -> TenantPolicy.requireAccess(a, "internalTool", "any"));
        assertEquals("internalTool", ex.getToolName());
    }

    @Test
    void tenantContext_runWith_restoresPriorValue() {
        TenantContext.set("outer");
        TenantContext.runWith("inner", () ->
            assertEquals("inner", TenantContext.get()));
        assertEquals("outer", TenantContext.get());
    }

    @Test
    void tenantContext_clear_resetsToEmpty() {
        TenantContext.set("acme");
        assertEquals("acme", TenantContext.get());
        TenantContext.clear();
        assertEquals(TenantContext.EMPTY, TenantContext.get());
    }

    @Test
    void tenantContext_setNull_normalisesToEmpty() {
        TenantContext.set(null);
        assertEquals(TenantContext.EMPTY, TenantContext.get());
    }

    // ---- Test helpers ----

    /**
     * Build a synthetic {@link McpTool} annotation without going through
     * the JDK dynamic proxy boilerplate manually.
     */
    private static McpTool annotation(boolean denyAll, String[] tenants) {
        return new McpTool() {
            @Override public Class<? extends Annotation> annotationType() { return McpTool.class; }
            @Override public String name() { return ""; }
            @Override public String title() { return ""; }
            @Override public String description() { return ""; }
            @Override public String mineType() { return "application/json"; }
            @Override public boolean readOnlyHint() { return false; }
            @Override public boolean destructiveHint() { return false; }
            @Override public boolean idempotentHint() { return false; }
            @Override public boolean openWorldHint() { return false; }
            @Override public boolean returnDirect() { return false; }
            @Override public boolean listChanged() { return true; }
            @Override public Class<? extends com.ai.plug.core.spec.callback.tool.McpCallToolResultConverter> converter() {
                return com.ai.plug.core.spec.callback.tool.DefaultMcpCallToolResultConverter.class;
            }
            @Override public String[] icons() { return new String[0]; }
            @Override public String metaJson() { return ""; }
            @Override public String resultType() { return "complete"; }
            @Override public long ttlMs() { return 0; }
            @Override public String cacheScope() { return ""; }
            @Override public String cacheWrapperKey() { return "_cacheable"; }
            @Override public String[] tenants() { return tenants; }
            @Override public boolean denyAll() { return denyAll; }
        };
    }
}
