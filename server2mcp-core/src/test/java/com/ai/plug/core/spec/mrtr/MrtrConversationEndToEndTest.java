/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.mrtr;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end demonstration of the MRTR multi-round state machine
 * implemented by {@link MrtrSessionStore}, {@link MrtrConversation},
 * {@link InMemoryMrtrSessionStore}, and {@link MrtrDriver}.
 *
 * <p>Scenario: an "order creation" tool that needs an address and a
 * payment method before it can produce a final {@code Order}.
 * Three rounds:
 * <ol>
 *   <li>Server asks the client for an address;</li>
 *   <li>Client provides the address; server asks for payment;</li>
 *   <li>Client provides payment; server produces the final Order.</li>
 * </ol>
 */
class MrtrConversationEndToEndTest {

    @Test
    void threeRoundConversation_addressThenPaymentThenOrder() {
        MrtrSessionStore store = new InMemoryMrtrSessionStore();

        // Round 1 — initial handler lacks address → emit elicitation
        MrtrDriver.Started round1 = MrtrDriver.start(store,
            ctx -> {
                assertThat(ctx.round()).isEqualTo(1);
                assertThat(ctx.partialArgs()).doesNotContainKey("address");
                return MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                    List.of(MrtrTypes.ElicitationInputRequest.create(
                        "Need shipping address",
                        Map.of("type", "object",
                            "properties", Map.of("zip", Map.of("type", "string")))))));
            },
            Map.of("item", "SKU-42", "qty", 2));
        assertThat(round1.requestState()).isNotBlank();
        assertThat(round1.isImmediate()).isFalse();
        assertThat(store.activeCount()).isEqualTo(1);

        // Round 2 — client provides address; server still lacks payment
        MrtrDriver.Resumed round2 = MrtrDriver.resume(store,
            round1.requestState(),
            MrtrTypes.InputResponses.of(Map.of(
                "elicitation", Map.of("zip", "94107"))),
            ctx -> {
                assertThat(ctx.round()).isEqualTo(2);
                assertThat(ctx.accumulatedResponses()).containsKey("elicitation");
                return MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                    List.of(MrtrTypes.ElicitationInputRequest.create(
                        "Need payment method",
                        Map.of("type", "object",
                            "properties", Map.of("card", Map.of("type", "string")))))));
            },
            Map.of("item", "SKU-42", "qty", 2));
        assertThat(round2.completed()).isFalse();
        assertThat(round2.nextRequest()).isNotNull();
        assertThat(store.activeCount()).isEqualTo(1);

        // Conversation still reflects the latest interim request
        MrtrConversation mid = store.get(round1.requestState()).orElseThrow();
        // Round counter: round 1 (initial) + 1 (after resume round 2)
        assertThat(mid.round()).isEqualTo(2);
        assertThat(mid.responseHistory()).hasSize(1);
        assertThat(mid.lastRequest().inputRequests()).hasSize(1);

        // Round 3 — client provides payment; server produces final Order
        MrtrDriver.Resumed round3 = MrtrDriver.resume(store,
            round1.requestState(),
            MrtrTypes.InputResponses.of(Map.of(
                "elicitation", Map.of("card", "tok_visa"))),
            ctx -> {
                assertThat(ctx.round()).isEqualTo(3);
                assertThat(ctx.accumulatedResponses()).containsKey("elicitation");
                // All info gathered — emit done.
                return MrtrDriver.done(Map.of(
                    "orderId", "ORD-9001",
                    "status", "confirmed"));
            },
            Map.of("item", "SKU-42", "qty", 2));
        assertThat(round3.completed()).isTrue();
        assertThat(round3.finalResult()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> order = (Map<String, Object>) round3.finalResult();
        assertThat(order).containsEntry("orderId", "ORD-9001");
        assertThat(order).containsEntry("status", "confirmed");

        // Session cleaned up after completion
        assertThat(store.activeCount()).isZero();
        assertThat(store.get(round1.requestState())).isEmpty();
    }

    @Test
    void handlerSkipsMrtr_immediateDoneNotStored() {
        MrtrSessionStore store = new InMemoryMrtrSessionStore();
        MrtrDriver.Started started = MrtrDriver.start(store,
            ctx -> MrtrDriver.done(Map.of("result", "no MRTR needed")),
            Map.of());
        assertThat(started.isImmediate()).isTrue();
        assertThat(started.requestState()).isNull();
        assertThat(started.finalResult()).isInstanceOf(Map.class);
        assertThat(store.activeCount()).isZero();
    }

    @Test
    void resumeWithUnknownRequestState_throws() {
        MrtrSessionStore store = new InMemoryMrtrSessionStore();
        assertThatThrownBy(() -> MrtrDriver.resume(store,
            "ghost-token",
            MrtrTypes.InputResponses.of(Map.of("elicitation", "x")),
            ctx -> MrtrDriver.done("unused"),
            Map.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ghost-token");
    }

    @Test
    void abandon_removesSession() {
        MrtrSessionStore store = new InMemoryMrtrSessionStore();
        MrtrDriver.Started started = MrtrDriver.start(store,
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.RootsInputRequest.create()))),
            Map.of());
        assertThat(store.activeCount()).isEqualTo(1);
        store.abandon(started.requestState());
        assertThat(store.activeCount()).isZero();
    }

    @Test
    void conversation_accumulatesResponseHistoryAcrossRounds() {
        MrtrSessionStore store = new InMemoryMrtrSessionStore();
        MrtrDriver.Started r1 = MrtrDriver.start(store,
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.JsonSchemaInputRequest.create(
                    "color", "Pick color", Map.of("type", "string"))))),
            Map.of());
        MrtrDriver.resume(store, r1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("color", "red")),
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.JsonSchemaInputRequest.create(
                    "size", "Pick size", Map.of("type", "string"))))),
            Map.of());
        MrtrDriver.resume(store, r1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("size", "M")),
            ctx -> MrtrDriver.done("selected: red/M"),
            Map.of());
        // Session cleared after final done.
        assertThat(store.activeCount()).isZero();
    }

    @Test
    void outcomeConstructor_rejectsBothFields() {
        assertThatThrownBy(() -> new MrtrDriver.Outcome(
            MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.RootsInputRequest.create())),
            "both"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MrtrDriver.Outcome(null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void outcome_isDoneReflectsFinalResult() {
        var next = new MrtrDriver.Outcome(
            MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.RootsInputRequest.create())),
            null);
        var done = new MrtrDriver.Outcome(null, "ok");
        assertThat(next.isDone()).isFalse();
        assertThat(done.isDone()).isTrue();
    }
}