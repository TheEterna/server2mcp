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

import com.ai.plug.core.spec.tasks.TasksEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Spring MVC controller exposing {@code /mcp/tasks} endpoints —
 * substitutes for protocol-2026-07-28 {@code tasks/get}, {@code tasks/list},
 * {@code tasks/cancel} JSON-RPC methods (Java SDK 2.0 lacks schemas).
 */
@RestController
@RequestMapping("/mcp/tasks")
public class TasksController {

    private final TasksEndpoint endpoint;

    public TasksController(TasksEndpoint endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint is required");
        }
        this.endpoint = endpoint;
    }

    @GetMapping
    public Map<String, Object> list() {
        return endpoint.handleList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable("id") String id) {
        return endpoint.handleGet(id);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable("id") String id,
                                       @RequestParam(value = "reason", required = false) String reason) {
        return endpoint.handleCancel(id, reason);
    }
}