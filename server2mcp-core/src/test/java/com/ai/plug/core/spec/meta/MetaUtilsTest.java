/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.meta;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetaUtilsTest {

    @Test
    void forwardTraceContext_emptySource() {
        assertThat(MetaUtils.forwardTraceContext(null)).isEmpty();
        assertThat(MetaUtils.forwardTraceContext(new HashMap<>())).isEmpty();
    }

    @Test
    void forwardTraceContext_copiesAllThreeReservedKeys() {
        Map<String, Object> source = Map.of(
            MetaUtils.TRACE_PARENT, "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01",
            MetaUtils.TRACE_STATE, "rojo=00f067aa0ba902b7",
            MetaUtils.BAGGAGE, "userId=alice",
            // unrelated keys should be filtered out
            "unrelated", "noise"
        );
        Map<String, Object> forward = MetaUtils.forwardTraceContext(source);
        assertThat(forward).hasSize(3);
        assertThat(forward.get(MetaUtils.TRACE_PARENT)).isEqualTo(source.get(MetaUtils.TRACE_PARENT));
        assertThat(forward.get(MetaUtils.TRACE_STATE)).isEqualTo(source.get(MetaUtils.TRACE_STATE));
        assertThat(forward.get(MetaUtils.BAGGAGE)).isEqualTo(source.get(MetaUtils.BAGGAGE));
    }

    @Test
    void forwardTraceContext_partialSourceStillForwards() {
        // Only traceparent present
        Map<String, Object> source = Map.of(MetaUtils.TRACE_PARENT, "00-x-x-01");
        Map<String, Object> forward = MetaUtils.forwardTraceContext(source);
        assertThat(forward).hasSize(1);
        assertThat(forward.get(MetaUtils.TRACE_PARENT)).isEqualTo("00-x-x-01");
    }

    @Test
    void merge_sourceTakesPrecedence() {
        Map<String, Object> base = Map.of("k1", "base", "k2", "base");
        Map<String, Object> source = Map.of("k2", "override", "k3", "new");
        Map<String, Object> merged = MetaUtils.merge(base, source);
        assertThat(merged).hasSize(3);
        assertThat(merged.get("k1")).isEqualTo("base");
        assertThat(merged.get("k2")).isEqualTo("override");
        assertThat(merged.get("k3")).isEqualTo("new");
    }

    @Test
    void merge_handlesNulls() {
        Map<String, Object> merged = MetaUtils.merge(null, null);
        assertThat(merged).isEmpty();
        Map<String, Object> fromBase = MetaUtils.merge(Map.of("a", "1"), null);
        assertThat(fromBase).hasSize(1);
        Map<String, Object> fromSource = MetaUtils.merge(null, Map.of("a", "1"));
        assertThat(fromSource).hasSize(1);
    }
}