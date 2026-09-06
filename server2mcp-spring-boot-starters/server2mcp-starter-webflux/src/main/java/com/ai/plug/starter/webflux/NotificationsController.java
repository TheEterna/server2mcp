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

import com.ai.plug.core.spec.change.NotificationsPollingEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebFlux counterpart of
 * {@code com.ai.plug.starter.webmvc.NotificationsController} — exposes
 * {@code GET /mcp/notifications?since=<cursor>} as a polling
 * substitute for protocol-2026-07-28 {@code subscriptions/listen}
 * (Java SDK 2.0 lacks schema). Reuses the same polling endpoint,
 * just bound to a reactive handler.
 */
@RestController
@RequestMapping("/mcp/notifications")
public class NotificationsController {

    private final NotificationsPollingEndpoint endpoint;

    public NotificationsController(NotificationsPollingEndpoint endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint is required");
        }
        this.endpoint = endpoint;
    }

    @GetMapping
    public Mono<Map<String, Object>> poll(
            @RequestParam(value = "since", required = false, defaultValue = "-1") long since) {
        return Mono.fromSupplier(() -> endpoint.handlePoll(since));
    }
}
