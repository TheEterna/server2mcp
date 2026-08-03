/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.test.test.controller;

import com.ai.plug.core.spec.mrtr.InMemoryMrtrSessionStore;
import com.ai.plug.core.spec.mrtr.MrtrDriver;
import com.ai.plug.core.spec.mrtr.MrtrSessionStore;
import com.ai.plug.core.spec.mrtr.MrtrTypes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * End-to-end MRTR (Multi Round-Trip Request) demo controller — exercises
 * the protocol-2026-07-28 SEP-2322 multi-round flow without any external
 * dependencies (no MySQL, no Redis). Three rounds:
 *
 * <pre>
 *   Round 1: client → POST /mcp-demo/order {"item": "SKU-1"}
 *            ← needs shipping address (elicitation)
 *   Round 2: client → POST /mcp-demo/order {"item": "SKU-1", "requestState": "...", "answers": {"street": "...", "city": "..."}}
 *            ← needs payment method (elicitation)
 *   Round 3: client → POST /mcp-demo/order {"item": "SKU-1", "requestState": "...", "answers": {"method": "card"}}
 *            ← terminal order confirmation
 * </pre>
 *
 * <p>This controller exists as a curl-friendly counterpart to the framework's
 * unit-tested {@code MrtrDriver} / {@code MrtrSessionStore} — it shows the
 * full HTTP shape a real MCP client would see.
 *
 * @author han
 * @time 2026/8/3
 */
@RestController
@RequestMapping("/mcp-demo")
public class MrtrDemoController {

    private final MrtrSessionStore store = new InMemoryMrtrSessionStore();

    @PostMapping("/order")
    public Map<String, Object> submitOrder(@RequestBody Map<String, Object> body) {
        String item = (String) body.getOrDefault("item", "UNKNOWN");
        String requestState = (String) body.get("requestState");
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = (Map<String, Object>) body.getOrDefault("answers", Map.of());

        if (requestState == null || requestState.isBlank()) {
            // Round 1 — start a session asking for shipping address.
            return startSession(item);
        }
        // Round 2+ — resume using the echoed state.
        return resumeSession(item, requestState, answers);
    }

    private Map<String, Object> startSession(String item) {
        MrtrDriver.Started started = MrtrDriver.start(store,
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create(
                    "Provide shipping address for " + item,
                    Map.of("type", "object",
                        "properties", Map.of(
                            "street", Map.of("type", "string"),
                            "city", Map.of("type", "string"))))))),
            Map.of("item", item));
        return Map.of(
            "status", "input_required",
            "requestState", started.requestState(),
            "inputRequests", started.nextRequest().inputRequests()
        );
    }

    private Map<String, Object> resumeSession(String item, String requestState,
                                               Map<String, Object> answers) {
        Optional<MrtrDriver.Resumed> result;
        try {
            result = Optional.of(MrtrDriver.resume(store, requestState,
                MrtrTypes.InputResponses.of(answers),
                ctx -> nextStepOrDone(ctx, item),
                Map.of("item", item)));
        } catch (RuntimeException ex) {
            return Map.of(
                "status", "error",
                "error", ex.getClass().getSimpleName(),
                "message", ex.getMessage()
            );
        }
        MrtrDriver.Resumed r = result.get();
        if (r.completed()) {
            return Map.of(
                "status", "complete",
                "order", Map.of(
                    "item", item,
                    "address", answers,
                    "payment", answers),
                "message", "Order created (simulated)"
            );
        }
        return Map.of(
            "status", "input_required",
            "requestState", requestState,
            "inputRequests", r.nextRequest().inputRequests()
        );
    }

    /** Inner state machine: round 2 → ask payment, round 3 → done. */
    private MrtrDriver.Outcome nextStepOrDone(MrtrDriver.RoundContext ctx, String item) {
        boolean hasAddress = ctx.accumulatedResponses().containsKey("street")
            && ctx.accumulatedResponses().containsKey("city");
        boolean hasPayment = ctx.accumulatedResponses().containsKey("method");
        if (hasAddress && !hasPayment) {
            return MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create(
                    "Choose payment method for " + item,
                    Map.of("type", "object",
                        "properties", Map.of(
                            "method", Map.of("type", "string",
                                "enum", List.of("card", "paypal", "cod"))))))));
        }
        if (hasAddress && hasPayment) {
            return MrtrDriver.done(Map.of(
                "item", item,
                "address", ctx.accumulatedResponses(),
                "payment", ctx.accumulatedResponses().get("method")));
        }
        return MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.ElicitationInputRequest.create(
                "Need shipping address first",
                Map.of("type", "object")))));
    }

    /** Operational endpoint — exposes live session count for debugging. */
    @GetMapping("/sessions")
    public Map<String, Object> sessions() {
        return Map.of("active", store.activeCount());
    }
}