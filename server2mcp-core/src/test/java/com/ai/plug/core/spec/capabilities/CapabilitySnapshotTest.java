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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilitySnapshotTest {

    @Test
    void snapshot_allEnabled_flagsSet() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        var snap = CapabilitySnapshot.from(caps);
        assertThat(snap.flags())
            .containsEntry("tools.listChanged", true)
            .containsEntry("resources.listChanged", true)
            .containsEntry("resources.subscribe", true)
            .containsEntry("prompts.listChanged", true);
    }

    @Test
    void snapshot_toolsOnly() {
        var caps = ServerCapabilitiesFactory.withToolsListChanged();
        var snap = CapabilitySnapshot.from(caps);
        assertThat(snap.flags()).containsEntry("tools.listChanged", true);
        assertThat(snap.flags()).doesNotContainKey("resources.subscribe");
        assertThat(snap.flags()).doesNotContainKey("prompts.listChanged");
    }

    @Test
    void snapshot_immutable() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        var snap = CapabilitySnapshot.from(caps);
        // Defensive copy — original flags map cannot be mutated
        assertThat(snap.flags()).isUnmodifiable();
    }

    @Test
    void diff_equalSnapshots_empty() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        var a = CapabilitySnapshot.from(caps);
        var b = CapabilitySnapshot.from(caps);
        assertThat(a.diff(b)).isEmpty();
    }

    @Test
    void diff_addedFlag() {
        var a = CapabilitySnapshot.from(
            ServerCapabilitiesFactory.withToolsListChanged());
        var b = CapabilitySnapshot.from(
            ServerCapabilitiesFactory.withListChangedAll());
        String diff = a.diff(b);
        assertThat(diff)
            .contains("+ added: resources.listChanged")
            .contains("+ added: resources.subscribe")
            .contains("+ added: prompts.listChanged");
    }

    @Test
    void diff_changedFlag() {
        // Both have resources, but one has subscribe=true and the other has subscribe=false
        var a = CapabilitySnapshot.from(
            ServerCapabilitiesFactory.withResourcesListChanged());
        // Manually create a capabilities with subscribe=false
        var noSub = io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.builder()
            .resources(false, true) // (subscribe=false, listChanged=true)
            .build();
        var b = CapabilitySnapshot.from(noSub);
        String diff = a.diff(b);
        assertThat(diff)
            .contains("~ changed: resources.subscribe: true -> false");
    }
}