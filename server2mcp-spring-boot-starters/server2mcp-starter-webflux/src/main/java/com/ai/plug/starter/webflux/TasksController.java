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

import com.ai.plug.core.spec.tasks.TasksEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebFlux counterpart of
 * {@code com.ai.plug.starter.webmvc.TasksController}. Exposes
 * {@code /mcp/tasks} endpoints as protocol-2026-07-28 substitutes
 * for {@code tasks/get}, {@code tasks/list}, {@code tasks/cancel}.
 *
 * <p>Each handler returns {@code Mono<Map<String, Object>>} so the
 * underlying endpoint can stay blocking (it does in-memory
 * {@code TaskStore} lookups) without pinning a Netty worker thread:
 * WebFlux schedules the call on its bounded-elastic scheduler.
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
    public Mono<Map<String, Object>> list() {
        return Mono.fromSupplier(endpoint::handleList);
    }

    @GetMapping("/{id}")
    public Mono<Map<String, Object>> get(@PathVariable("id") String id) {
        return Mono.fromSupplier(() -> endpoint.handleGet(id));
    }

    @PostMapping("/{id}/cancel")
    public Mono<Map<String, Object>> cancel(@PathVariable("id") String id,
                                             @RequestParam(value = "reason", required = false) String reason) {
        return Mono.fromSupplier(() -> endpoint.handleCancel(id, reason));
    }
}
