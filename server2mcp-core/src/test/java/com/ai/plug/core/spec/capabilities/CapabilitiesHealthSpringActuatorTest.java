/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.capabilities;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilitiesHealthSpringActuatorTest {

    @Test
    void healthyCaps_UP() {
        var actuator = new CapabilitiesHealthSpringActuator(
            new CapabilitiesHealthReportActuator(
                ServerCapabilitiesFactory::withListChangedAll));
        var health = actuator.health();
        assertThat(health.status()).isEqualTo("UP");
        assertThat(health.isUp()).isTrue();
        assertThat(health.details().issueCount()).isZero();
    }

    @Test
    void unhealthyCaps_DOWN() {
        var actuator = new CapabilitiesHealthSpringActuator(
            new CapabilitiesHealthReportActuator(
                () -> McpSchema.ServerCapabilities.builder().build()));
        var health = actuator.health();
        assertThat(health.status()).isEqualTo("DOWN");
        assertThat(health.isUp()).isFalse();
        assertThat(health.details().issueCount()).isGreaterThan(0);
    }

    @Test
    void constructor_nullInnerRejected() {
        assertThatThrownBy(() -> new CapabilitiesHealthSpringActuator(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void health_record_isUp() {
        var health = new CapabilitiesHealthSpringActuator.Health("UP", null);
        assertThat(health.isUp()).isTrue();
        assertThat(new CapabilitiesHealthSpringActuator.Health("DOWN", null).isUp())
            .isFalse();
    }
}