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

import com.ai.plug.core.spec.jsonrpc.JsonRpcResponse;
import com.ai.plug.core.spec.jsonrpc.JsonRpcRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Spring MVC controller that exposes the protocol-2026-07-28 JSON-RPC
 * endpoint at {@code POST /mcp/jsonrpc}. Accepts a single JSON-RPC
 * envelope and returns a {@link JsonRpcResponse}.
 *
 * <p>This is the direct replacement for SDK 2.0's missing JSON-RPC routes
 * for protocol-2026-07-28-only methods ({@code server/discover},
 * {@code tasks/*}, {@code subscriptions/listen}). Once Java MCP SDK
 * ≥ 3.0.0 ships its native router, this controller becomes a fallback
 * (zero behavior change — the wire shape is identical).
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
    public JsonRpcResponse dispatch(@RequestBody Map<String, Object> body) {
        logger.debug("JSON-RPC dispatch: method={}", body.get("method"));
        JsonRpcResponse response = router.dispatchRaw(body);
        if (response == null) {
            return JsonRpcResponse.error(
                JsonRpcResponse.JsonRpcError.of(
                    JsonRpcResponse.JsonRpcError.INVALID_REQUEST,
                    "Body must be a JSON-RPC request envelope"),
                null);
        }
        return response;
    }
}