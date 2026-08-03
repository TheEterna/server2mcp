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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trivial smoke test — verifies the framework core classes (and the
 * starter's controllers) are reachable on the test classpath. The
 * full MockMvc integration test lives in
 * {@link ProtocolEndpointsIntegrationTest} but is split out so a
 * failure there doesn't block this baseline check.
 */
class EndpointsSmokeTest {

    @Test
    void starter_classesAreReachable() {
        // If any of these fail to resolve, the dependency wiring is broken.
        assertThat(DiscoverController.class).isNotNull();
        assertThat(TasksController.class).isNotNull();
        assertThat(NotificationsController.class).isNotNull();
        assertThat(AugmentedPromptsController.class).isNotNull();
        assertThat(ProtocolEndpointsAutoConfiguration.class).isNotNull();
    }
}