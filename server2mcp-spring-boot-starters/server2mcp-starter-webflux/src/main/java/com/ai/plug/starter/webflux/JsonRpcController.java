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

import com.ai.plug.core.spec.jsonrpc.JsonRpcResponse;
import com.ai.plug.core.spec.jsonrpc.JsonRpcRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebFlux counterpart of
 * {@code com.ai.plug.starter.webmvc.JsonRpcController} — exposes the
 * protocol-2026-07-28 JSON-RPC endpoint at {@code POST /mcp/jsonrpc}
 * on the reactive stack. Returns {@link Mono} so routing stays
 * non-blocking under Netty.
 *
 * <p>Identical wire contract as the WebMVC version: clients
 * (curl / SDK) can hit either implementation with the same envelope
 * and the same response shape — only the runtime stack differs.
 *
 * @author han
 * @time 2026/8/3
 */
@RestController
public class JsonRpcController {

    private static final Logger logger = LoggerFactory.getLogger(JsonRpcController.class);

    private final JsonRpcRouter router;

    public JsonRpcController(JsonRpcRouter router) {
        this.router = router;
    }

    @PostMapping(value = "/mcp/jsonrpc",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonRpcResponse> dispatch(@RequestBody Map<String, Object> body) {
        logger.debug("JSON-RPC dispatch: method={}", body.get("method"));
        return Mono.fromCallable(() -> {
            JsonRpcResponse response = router.dispatchRaw(body);
            if (response == null) {
                return JsonRpcResponse.error(
                    JsonRpcResponse.JsonRpcError.of(
                        JsonRpcResponse.JsonRpcError.INVALID_REQUEST,
                        "Body must be a JSON-RPC request envelope"),
                    null);
            }
            return response;
        });
    }
}
