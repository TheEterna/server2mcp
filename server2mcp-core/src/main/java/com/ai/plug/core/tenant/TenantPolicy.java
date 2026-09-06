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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Static decision table: given a tool's tenant policy (the
 * {@link McpTool#tenants()} allow-list and {@link McpTool#denyAll()}
 * flag) and the active tenant ID, decide whether the tool is visible
 * / callable.
 *
 * <p>Rules (in priority order):
 * <ol>
 *   <li>{@code denyAll=true} → always hidden, throws
 *       {@link McpAccessDeniedException} on call.</li>
 *   <li>{@code tenants()} empty → visible to every tenant (default).</li>
 *   <li>{@code tenants()} non-empty → visible only to the listed
 *       tenant IDs (exact-string match, case-sensitive).</li>
 * </ol>
 *
 * <p>The {@code activeTenantId} parameter is treated as opaque —
 * passing {@link TenantContext#EMPTY} is allowed and matches the
 * "no tenant" case (e.g. public tools).
 */
public final class TenantPolicy {

    private TenantPolicy() {
    }

    /** Visibility check used during {@code tools/list}. */
    public static boolean isVisible(McpTool annotation, String activeTenantId) {
        if (annotation == null) {
            return true;
        }
        if (annotation.denyAll()) {
            return false;
        }
        String[] allowed = annotation.tenants();
        if (allowed.length == 0) {
            return true;
        }
        if (activeTenantId == null || activeTenantId.isEmpty()) {
            return false;
        }
        for (String t : allowed) {
            if (t.equals(activeTenantId)) {
                return true;
            }
        }
        return false;
    }

    /** Hard call-time check. Throws {@link McpAccessDeniedException}
     *  if the active tenant is not allowed. */
    public static void requireAccess(McpTool annotation, String toolName, String activeTenantId) {
        if (annotation == null) {
            return;
        }
        if (annotation.denyAll()) {
            throw new McpAccessDeniedException(toolName, activeTenantId,
                "Tool '" + toolName + "' is marked denyAll and cannot be invoked by any tenant");
        }
        String[] allowed = annotation.tenants();
        if (allowed.length == 0) {
            return; // No restriction.
        }
        if (activeTenantId == null || activeTenantId.isEmpty()) {
            throw new McpAccessDeniedException(toolName, activeTenantId,
                "Tool '" + toolName + "' requires an authenticated tenant; none was provided");
        }
        for (String t : allowed) {
            if (t.equals(activeTenantId)) {
                return;
            }
        }
        throw new McpAccessDeniedException(toolName, activeTenantId);
    }

    /** Returns the immutable allow-list for diagnostics / debugging. */
    public static Set<String> allowList(McpTool annotation) {
        if (annotation == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(annotation.tenants()));
    }
}
