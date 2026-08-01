/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.resulttype;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationShimTest {

    @Test
    void sdkSupports_resultType_isFalseOnSDK2_0() {
        // SDK 2.0 CallToolResult record does NOT have a resultType() accessor
        // (only meta() and the content/error/structured fields). Migration shim
        // must detect this so the framework keeps using meta-map.
        assertThat(MigrationShim.sdkSupportsResultType()).isFalse();
    }

    @Test
    void sdkSupports_experimental_isTrueOnSDK2_0() {
        // ServerCapabilities.experimental() is exposed in SDK 2.0 already.
        // (We keep the check for symmetry / future-proofing.)
        assertThat(MigrationShim.sdkSupportsExtensions()).isTrue();
    }

    @Test
    void sdkSupportsWireFields_isFalseOnSDK2_0() {
        // Until ALL four fields are SDK-side, the framework keeps the meta
        // wrapping path. Today this returns false (resultType/ttlMs/cacheScope
        // are absent).
        assertThat(MigrationShim.sdkSupportsWireFields()).isFalse();
    }

    @Test
    void allFourSupportFlags_individuallyChecked() {
        // Independent checks so callers can opt in selectively
        // (e.g. if SDK 2.1 ships resultType but not extensions, callers can
        // still use SDK resultType path while keeping extensions in experimental map).
        // resultType: not yet
        assertThat(MigrationShim.sdkSupportsResultType()).isFalse();
        // ttlMs: not yet
        assertThat(MigrationShim.sdkSupportsTtlMs()).isFalse();
        // cacheScope: not yet
        assertThat(MigrationShim.sdkSupportsCacheScope()).isFalse();
        // extensions (via experimental): yes
        assertThat(MigrationShim.sdkSupportsExtensions()).isTrue();
    }
}