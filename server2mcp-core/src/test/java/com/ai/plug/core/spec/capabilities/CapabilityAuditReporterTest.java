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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityAuditReporterTest {

    @Test
    void firstSnapshot_noDiff() {
        AtomicReference<String> emitted = new AtomicReference<>();
        CapabilityAuditReporter reporter = new CapabilityAuditReporter(
            ServerCapabilitiesFactory::withListChangedAll,
            emitted::set);
        String diff = reporter.snapshotAndReport();
        // No previous -> diff empty
        assertThat(diff).isEmpty();
        // Sink was still called with a status line
        assertThat(emitted.get()).contains("no changes");
    }

    @Test
    void secondSnapshot_sameCapabilities_emptyDiff() {
        AtomicReference<String> emitted = new AtomicReference<>();
        CapabilityAuditReporter reporter = new CapabilityAuditReporter(
            ServerCapabilitiesFactory::withListChangedAll,
            emitted::set);
        reporter.snapshotAndReport();
        emitted.set(null);
        String diff = reporter.snapshotAndReport();
        assertThat(diff).isEmpty();
        assertThat(emitted.get()).contains("no changes");
    }

    @Test
    void secondSnapshot_changedCapabilities_emitsDiff() {
        AtomicReference<String> emitted = new AtomicReference<>();
        // Source returns different capabilities on each call
        AtomicInteger callCount = new AtomicInteger();
        CapabilityAuditReporter reporter = new CapabilityAuditReporter(
            () -> {
                if (callCount.incrementAndGet() == 1) {
                    return ServerCapabilitiesFactory.withToolsListChanged();
                }
                return ServerCapabilitiesFactory.withListChangedAll();
            },
            emitted::set);
        reporter.snapshotAndReport();
        emitted.set(null);
        String diff = reporter.snapshotAndReport();
        assertThat(diff)
            .contains("+ added: resources.listChanged")
            .contains("+ added: resources.subscribe")
            .contains("+ added: prompts.listChanged");
        assertThat(emitted.get()).contains("capability change detected");
    }

    @Test
    void resetBaseline_forcesInitialSnapshot() {
        AtomicReference<String> emitted = new AtomicReference<>();
        CapabilityAuditReporter reporter = new CapabilityAuditReporter(
            ServerCapabilitiesFactory::withListChangedAll,
            emitted::set);
        reporter.snapshotAndReport();
        reporter.resetBaseline();
        emitted.set(null);
        // After reset, the next call should treat the current state as initial
        String diff = reporter.snapshotAndReport();
        assertThat(diff).isEmpty();
    }

    @Test
    void currentFlags_returnsLastSnapshot() {
        CapabilityAuditReporter reporter = new CapabilityAuditReporter(
            ServerCapabilitiesFactory::withListChangedAll);
        reporter.snapshotAndReport();
        assertThat(reporter.currentFlags())
            .containsEntry("tools.listChanged", true)
            .containsEntry("resources.listChanged", true)
            .containsEntry("prompts.listChanged", true);
    }

    @Test
    void currentFlags_emptyBeforeFirstSnapshot() {
        CapabilityAuditReporter reporter = new CapabilityAuditReporter(
            ServerCapabilitiesFactory::withListChangedAll);
        assertThat(reporter.currentFlags()).isEmpty();
    }
}