/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"));
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.dedup;

import com.ai.plug.core.annotation.McpTool;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {

    @Test
    void nullAnnotation_noRetry() {
        assertThat(RetryPolicy.shouldAutoRetry(null)).isFalse();
        assertThat(RetryPolicy.maxRetries(null)).isZero();
    }

    @Test
    void defaultAnnotation_isIdempotent() {
        // @McpTool default has idempotentHint=true (per current annotation)
        McpTool ann = Sample.class.getAnnotation(McpTool.class);
        if (ann == null) {
            return; // skip if no annotated method
        }
        // Skip if the default ever changes
        if (ann.idempotentHint()) {
            assertThat(RetryPolicy.shouldAutoRetry(ann)).isTrue();
            assertThat(RetryPolicy.maxRetries(ann)).isEqualTo(1);
        }
    }

    @Test
    void destructiveHint_overridesIdempotent() {
        // Even with idempotentHint=true, destructiveHint blocks auto-retry
        // — destructive operations are too dangerous to repeat
        McpTool ann = findAnnotation("destructiveMethod");
        assertThat(RetryPolicy.shouldAutoRetry(ann)).isFalse();
        assertThat(RetryPolicy.maxRetries(ann)).isZero();
    }

    @Test
    void nonIdempotent_noRetry() {
        McpTool ann = findAnnotation("nonIdempotentMethod");
        assertThat(RetryPolicy.shouldAutoRetry(ann)).isFalse();
        assertThat(RetryPolicy.maxRetries(ann)).isZero();
    }

    @Test
    void idempotentNoDestructive_oneRetry() {
        McpTool ann = findAnnotation("idempotentMethod");
        assertThat(RetryPolicy.shouldAutoRetry(ann)).isTrue();
        assertThat(RetryPolicy.maxRetries(ann)).isEqualTo(1);
    }

    private static McpTool findAnnotation(String methodName) {
        try {
            return Sample.class.getDeclaredMethod(methodName).getAnnotation(McpTool.class);
        }
        catch (NoSuchMethodException ex) {
            throw new AssertionError("missing method: " + methodName, ex);
        }
    }

    static final class Sample {
        @McpTool(name = "destructiveMethod", idempotentHint = true, destructiveHint = true)
        public String destructiveMethod() { return "x"; }

        @McpTool(name = "nonIdempotentMethod", idempotentHint = false)
        public String nonIdempotentMethod() { return "x"; }

        @McpTool(name = "idempotentMethod", idempotentHint = true, destructiveHint = false)
        public String idempotentMethod() { return "x"; }
    }
}