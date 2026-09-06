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
 * Thrown when a tool invocation is rejected because the active
 * tenant is not on the tool's allow-list (or because the tool is
 * marked {@code denyAll=true}). Mapped to an HTTP 403 by the
 * starter controllers and to a JSON-RPC error code of
 * {@code -32003} (server-defined) by the JSON-RPC router.
 */
public class McpAccessDeniedException extends RuntimeException {

    private final String toolName;
    private final String tenantId;

    public McpAccessDeniedException(String toolName, String tenantId) {
        super("Tenant '" + tenantId + "' is not allowed to call tool '" + toolName + "'");
        this.toolName = toolName;
        this.tenantId = tenantId;
    }

    public McpAccessDeniedException(String toolName, String tenantId, String message) {
        super(message);
        this.toolName = toolName;
        this.tenantId = tenantId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getTenantId() {
        return tenantId;
    }
}
