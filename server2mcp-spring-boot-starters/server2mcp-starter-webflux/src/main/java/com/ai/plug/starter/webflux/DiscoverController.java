/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.starter.webflux;

import com.ai.plug.core.spec.discover.DiscoverEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebFlux controller exposing {@code GET /mcp/discover} — the
 * reactive-stack counterpart of
 * {@code com.ai.plug.starter.webmvc.DiscoverController}. Same wire
 * shape, returns {@code Mono<Map<String, Object>>} so the runtime
 * doesn't block the Netty event loop.
 *
 * <p>Mounted automatically by
 * {@link ProtocolEndpointsAutoConfiguration}.
 */
@RestController
@RequestMapping("/mcp")
public class DiscoverController {

    private final DiscoverEndpoint endpoint;

    public DiscoverController(DiscoverEndpoint endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint is required");
        }
        this.endpoint = endpoint;
    }

    @GetMapping("/discover")
    public Mono<Map<String, Object>> discover() {
        return Mono.fromSupplier(endpoint::handle);
    }
}
