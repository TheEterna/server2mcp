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

import com.ai.plug.core.spec.tasks.AugmentedPromptEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebFlux counterpart of
 * {@code com.ai.plug.starter.webmvc.AugmentedPromptsController} —
 * exposes {@code /mcp/tasks/{id}/augmented-prompts} for the
 * protocol-2026-07-28 {@code tasks/augmented-prompt} method.
 */
@RestController
@RequestMapping("/mcp/tasks/{id}/augmented-prompts")
public class AugmentedPromptsController {

    private final AugmentedPromptEndpoint endpoint;

    public AugmentedPromptsController(AugmentedPromptEndpoint endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint is required");
        }
        this.endpoint = endpoint;
    }

    @GetMapping
    public Mono<Map<String, Object>> list(@PathVariable("id") String id) {
        return Mono.fromSupplier(() -> endpoint.handleList(id));
    }

    @PostMapping("/drain")
    public Mono<Map<String, Object>> drain(@PathVariable("id") String id) {
        return Mono.fromSupplier(() -> endpoint.handleDrain(id));
    }
}
