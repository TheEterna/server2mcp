/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"));
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.discover;

import com.ai.plug.core.spec.discover.DiscoverTypes.DiscoverRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscoverRequestTest {

    @Test
    void constructor_validProtocol() {
        DiscoverRequest req = DiscoverRequest.of("2026-07-28");
        assertThat(req.preferredProtocol()).isEqualTo("2026-07-28");
    }

    @Test
    void constructor_nullProtocolRejected() {
        assertThatThrownBy(() -> new DiscoverRequest(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("preferredProtocol is required");
    }

    @Test
    void constructor_blankProtocolRejected() {
        assertThatThrownBy(() -> new DiscoverRequest("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void supportsAllKnownProtocolVersions() {
        // Placeholder for documenting the protocol version negotiation contract
        DiscoverRequest r1 = DiscoverRequest.of("2025-11-25");
        DiscoverRequest r2 = DiscoverRequest.of("2026-07-28");
        assertThat(r1.preferredProtocol()).isEqualTo("2025-11-25");
        assertThat(r2.preferredProtocol()).isEqualTo("2026-07-28");
    }
}