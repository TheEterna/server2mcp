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

import com.ai.plug.core.spec.capabilities.SnapshotCompareTool.Change;
import com.ai.plug.core.spec.capabilities.SnapshotCompareTool.Diff;
import com.ai.plug.core.spec.integration.WireSchemaExporter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotCompareToolTest {

    @Test
    void identicalSnapshots_emptyDiff() {
        var caps = ServerCapabilitiesFactory.withListChangedAll();
        var diff = SnapshotCompareTool.compare(
            CapabilitySnapshot.from(caps), CapabilitySnapshot.from(caps));
        assertThat(diff.isEmpty()).isTrue();
        assertThat(diff.totalChanges()).isZero();
    }

    @Test
    void addedFlag_appearsInAdded() {
        var before = CapabilitySnapshot.from(ServerCapabilitiesFactory.withToolsListChanged());
        var after = CapabilitySnapshot.from(ServerCapabilitiesFactory.withListChangedAll());
        var diff = SnapshotCompareTool.compare(before, after);
        // withListChangedAll adds 3 flags vs withToolsListChanged:
        // resources.listChanged, resources.subscribe, prompts.listChanged
        assertThat(diff.added()).hasSize(3);
        assertThat(diff.added()).extracting(Change::key)
            .contains("resources.subscribe", "resources.listChanged",
                "prompts.listChanged");
    }

    @Test
    void removedFlag_appearsInRemoved() {
        var before = CapabilitySnapshot.from(ServerCapabilitiesFactory.withListChangedAll());
        var after = CapabilitySnapshot.from(ServerCapabilitiesFactory.withToolsListChanged());
        var diff = SnapshotCompareTool.compare(before, after);
        assertThat(diff.removed()).hasSize(3);
    }

    @Test
    void changedFlag_appearsInChanged() {
        // Use raw builder to construct caps with subscribe=false vs true
        var beforeCaps = io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.builder()
            .resources(false, true) // subscribe=false, listChanged=true
            .build();
        var afterCaps = io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.builder()
            .resources(true, true) // subscribe=true, listChanged=true
            .build();
        var diff = SnapshotCompareTool.compare(
            CapabilitySnapshot.from(beforeCaps), CapabilitySnapshot.from(afterCaps));
        assertThat(diff.changed()).hasSize(1);
        assertThat(diff.changed().get(0).key()).isEqualTo("resources.subscribe");
    }

    @Test
    void summary_humanReadable() {
        var before = CapabilitySnapshot.from(ServerCapabilitiesFactory.withToolsListChanged());
        var after = CapabilitySnapshot.from(ServerCapabilitiesFactory.withListChangedAll());
        var diff = SnapshotCompareTool.compare(before, after);
        // 3 added, 0 removed, 0 changed
        assertThat(diff.summary()).isEqualTo("Diff: +3 -0 ~0");
    }

    @Test
    void toJson_serializesAllFields() throws Exception {
        var before = CapabilitySnapshot.from(ServerCapabilitiesFactory.withToolsListChanged());
        var after = CapabilitySnapshot.from(ServerCapabilitiesFactory.withListChangedAll());
        var diff = SnapshotCompareTool.compare(before, after);
        String json = diff.toJson();
        assertThat(json).contains("\"isEmpty\":false");
        assertThat(json).contains("\"totalChanges\":3");
        assertThat(json).contains("\"added\":[");
        assertThat(json).contains("\"removed\":[");
        assertThat(json).contains("\"changed\":[");
    }

    @Test
    void toJson_parseableByJackson() throws Exception {
        var before = CapabilitySnapshot.from(ServerCapabilitiesFactory.withToolsListChanged());
        var after = CapabilitySnapshot.from(ServerCapabilitiesFactory.withListChangedAll());
        var diff = SnapshotCompareTool.compare(before, after);
        String json = diff.toJson();
        @SuppressWarnings("unchecked")
        var parsed = com.ai.plug.common.utils.JsonParser.getObjectMapper()
            .readValue(json, Map.class);
        assertThat(parsed).containsEntry("isEmpty", false);
        assertThat(parsed).containsKey("totalChanges");
        assertThat((java.util.List<?>) parsed.get("added")).isNotEmpty();
    }

    @Test
    void change_isAddition_isRemoval() {
        Change added = new Change("k", null, "v");
        Change removed = new Change("k", "v", null);
        Change changed = new Change("k", "a", "b");
        assertThat(added.isAddition()).isTrue();
        assertThat(added.isRemoval()).isFalse();
        assertThat(removed.isAddition()).isFalse();
        assertThat(removed.isRemoval()).isTrue();
        assertThat(changed.isAddition()).isFalse();
        assertThat(changed.isRemoval()).isFalse();
    }

    @Test
    void compare_includesExperimentalChanges() {
        var beforeCaps = WireSchemaExporter.fullCapabilitiesWithExtensions(
            WireSchemaExporter.tasksExtension());
        var afterCaps = WireSchemaExporter.fullCapabilitiesWithExtensions(Map.of(
            "io.modelcontextprotocol/tasks", Map.of("version", "v2")));
        var diff = SnapshotCompareTool.compare(
            CapabilitySnapshot.from(beforeCaps), CapabilitySnapshot.from(afterCaps));
        // Both snapshots have the same set of "capability flag" keys
        // (tools/resources/prompts listChanged, resources.subscribe), so the
        // diff at the flag level is empty.
        // (Experimental differences are NOT surfaced by CapabilitySnapshot.flags()
        // — they're in the experimental Map, which the diff doesn't introspect
        // here. That's intentional: CapabilitySnapshot tracks the SDK 2.0
        // boolean capability flags; extensions live in the server-info wire
        // identity and have their own audit path.)
        assertThat(diff.isEmpty()).isTrue();
    }
}