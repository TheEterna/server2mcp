/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.starter.webmvc;

import com.ai.plug.core.spec.change.NotificationsPollingEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Spring MVC controller exposing {@code GET /mcp/notifications} —
 * HTTP polling substitute for protocol-2026-07-28
 * {@code subscriptions/listen} SSE long-push (Java SDK 2.0 lacks schema).
 *
 * <p>Clients poll {@code /mcp/notifications?since=&lt;cursor&gt;} to
 * fetch change events. {@code since=-1} returns all buffered events.
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
    public Map<String, Object> poll(
            @RequestParam(value = "since", required = false, defaultValue = "-1") long since) {
        return endpoint.handlePoll(since);
    }
}