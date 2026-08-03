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

import com.ai.plug.core.spec.mrtr.MrtrCallbackHints.MrtrInputResponses;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the callback-boundary helpers that let {@link MrtrDriver} slot
 * into the existing tool method pipeline without forcing every integrator
 * to write manual session bookkeeping.
 */
class MrtrCallbackHintsTest {

    /** Demo tool method with the {@link MrtrInputResponses} parameter. */
    public Map<String, Object> demoTool(
            String item,
            @MrtrInputResponses Map<String, Object> responses) {
        return Map.of("item", item, "received", responses);
    }

    /** Demo tool method without the annotation. */
    public String plainTool(String item) {
        return item;
    }

    private Parameter[] paramsOf(String methodName) throws NoSuchMethodException {
        Method m = getClass().getDeclaredMethod(methodName, paramTypes(methodName));
        return m.getParameters();
    }

    private Class<?>[] paramTypes(String methodName) {
        if (methodName.equals("demoTool")) {
            return new Class<?>[]{String.class, Map.class};
        }
        return new Class<?>[]{String.class};
    }

    @Test
    void findInputResponsesIndex_returnsMinusOne_whenAbsent() throws Exception {
        Parameter[] params = paramsOf("plainTool");
        assertThat(MrtrCallbackHints.findInputResponsesIndex(params)).isEqualTo(-1);
    }

    @Test
    void findInputResponsesIndex_returnsIndex_whenAnnotated() throws Exception {
        Parameter[] params = paramsOf("demoTool");
        assertThat(MrtrCallbackHints.findInputResponsesIndex(params)).isEqualTo(1);
    }

    @Test
    void resolveInputResponses_returnsNull_whenNoAnnotation() throws Exception {
        Parameter[] params = paramsOf("plainTool");
        var store = new InMemoryMrtrSessionStore();
        assertThat(MrtrCallbackHints.resolveInputResponses(params, "x", store))
            .isNull();
    }

    @Test
    void resolveInputResponses_returnsNull_whenRequestStateBlank() throws Exception {
        Parameter[] params = paramsOf("demoTool");
        var store = new InMemoryMrtrSessionStore();
        assertThat(MrtrCallbackHints.resolveInputResponses(params, "", store))
            .isNull();
        assertThat(MrtrCallbackHints.resolveInputResponses(params, null, store))
            .isNull();
    }

    @Test
    void resolveInputResponses_returnsLatestAnswers_whenSessionExists() throws Exception {
        var store = new InMemoryMrtrSessionStore();
        // Round 1: handler returns InputRequiredResult
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.RootsInputRequest.create()));
        String token = MrtrCallbackHints.startSessionAfterInputRequired(irr, null, store);
        // Round 2: client responds
        store.append(token, MrtrTypes.InputResponses.of(Map.of("roots", List.of("file:/x"))));
        // Resolve via the annotated parameter
        Parameter[] params = paramsOf("demoTool");
        Map<String, Object> responses = MrtrCallbackHints.resolveInputResponses(
            params, token, store);
        assertThat(responses).isNotNull();
        assertThat(responses).containsKey("roots");
    }

    @Test
    void startSessionAfterInputRequired_returnsNullForNonMrtrResult() {
        var store = new InMemoryMrtrSessionStore();
        String token = MrtrCallbackHints.startSessionAfterInputRequired(
            "plain result", null, store);
        assertThat(token).isNull();
        assertThat(store.activeCount()).isZero();
    }

    @Test
    void startSessionAfterInputRequired_reusesRequestStateWhenProvided() {
        var store = new InMemoryMrtrSessionStore();
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.RootsInputRequest.create()));
        String token = MrtrCallbackHints.startSessionAfterInputRequired(irr, "user-token-7", store);
        assertThat(token).isEqualTo("user-token-7");
        assertThat(store.activeCount()).isEqualTo(1);
        // Second call with same token should not create a second session
        MrtrCallbackHints.startSessionAfterInputRequired(irr, "user-token-7", store);
        assertThat(store.activeCount()).isEqualTo(1);
    }

    @Test
    void startSessionAfterInputRequired_assignsUuidWhenNoToken() {
        var store = new InMemoryMrtrSessionStore();
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.RootsInputRequest.create()));
        String token = MrtrCallbackHints.startSessionAfterInputRequired(irr, null, store);
        assertThat(token).isNotBlank();
        assertThat(token).hasSize(36); // UUID format
    }

    @Test
    void completeIfTerminal_clearsSession() {
        var store = new InMemoryMrtrSessionStore();
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.RootsInputRequest.create()));
        String token = MrtrCallbackHints.startSessionAfterInputRequired(irr, null, store);
        assertThat(store.activeCount()).isEqualTo(1);
        boolean completed = MrtrCallbackHints.completeIfTerminal("done", token, store);
        assertThat(completed).isTrue();
        assertThat(store.activeCount()).isZero();
    }

    @Test
    void completeIfTerminal_noopWhenResultIsInputRequired() {
        var store = new InMemoryMrtrSessionStore();
        MrtrTypes.InputRequiredResult irr = MrtrTypes.InputRequiredResult.of(
            List.of(MrtrTypes.RootsInputRequest.create()));
        String token = MrtrCallbackHints.startSessionAfterInputRequired(irr, null, store);
        boolean completed = MrtrCallbackHints.completeIfTerminal(irr, token, store);
        assertThat(completed).isFalse();
        assertThat(store.activeCount()).isEqualTo(1);
    }

    @Test
    void completeIfTerminal_noopWhenRequestStateBlank() {
        var store = new InMemoryMrtrSessionStore();
        boolean completed = MrtrCallbackHints.completeIfTerminal("done", null, store);
        assertThat(completed).isFalse();
    }
}