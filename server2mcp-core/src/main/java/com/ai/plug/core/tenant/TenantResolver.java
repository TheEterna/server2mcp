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
 * SPI for resolving the current tenant ID from the surrounding
 * transport. Implementations are picked up by Spring as beans; the
 * framework uses the highest-priority one.
 *
 * <p>Built-in default: {@link HeaderTenantResolver} reads the
 * {@code X-Mcp-Tenant} HTTP header. Override by registering a custom
 * bean — for example to pull tenant from a JWT claim, a session
 * attribute, or an OAuth2 scope.
 *
 * <p>Returning {@link TenantContext#EMPTY} means "no tenant bound" —
 * the framework treats this as the default-tenant case, which is
 * compatible with single-tenant deployments.
 */
@FunctionalInterface
public interface TenantResolver {

    /**
     * Resolve the current tenant ID. Implementations should never
     * return {@code null}; use {@link TenantContext#EMPTY} instead.
     */
    String resolve();
}
