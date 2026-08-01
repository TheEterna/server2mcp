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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionNegotiatorTest {

    @Test
    void negotiate_exactMatchReturnsSameVersion() {
        assertThat(VersionNegotiator.negotiate("2025-11-25"))
            .isEqualTo("2025-11-25");
        assertThat(VersionNegotiator.negotiate("2026-07-28"))
            .isEqualTo("2026-07-28");
    }

    @Test
    void negotiate_nullOrBlankReturnsLatestSupported() {
        assertThat(VersionNegotiator.negotiate(null))
            .isEqualTo(VersionNegotiator.PREFERRED_VERSION);
        assertThat(VersionNegotiator.negotiate(""))
            .isEqualTo(VersionNegotiator.PREFERRED_VERSION);
        assertThat(VersionNegotiator.negotiate("   "))
            .isEqualTo(VersionNegotiator.PREFERRED_VERSION);
    }

    @Test
    void negotiate_noCompatibleVersionReturnsNull() {
        // 2024-11-05 is not in supported list
        assertThat(VersionNegotiator.negotiate("2024-11-05")).isNull();
    }

    @Test
    void negotiate_customServerSupportedOverridesDefault() {
        // Custom supported set: only 2024-11-05
        var result = VersionNegotiator.negotiate("2024-11-05",
            List.of("2024-11-05"));
        assertThat(result).isEqualTo("2024-11-05");
    }

    @Test
    void negotiate_emptyServerSupportedFallsBackToDefault() {
        // Empty list → use default SUPPORTED_VERSIONS
        assertThat(VersionNegotiator.negotiate("2025-11-25", List.of()))
            .isEqualTo("2025-11-25");
    }

    @Test
    void negotiate_majorMinorMatchWithPatchSuffix() {
        // Future patch suffix form: 2025-11-25-rc1 should match 2025-11-25
        assertThat(VersionNegotiator.negotiate("2025-11-25-rc1"))
            .isEqualTo("2025-11-25");
    }

    @Test
    void supportedVersions_includesLatest2026() {
        assertThat(VersionNegotiator.SUPPORTED_VERSIONS)
            .contains("2025-11-25", "2026-07-28");
    }

    @Test
    void stripPatch_handlesCanonicalDate() {
        // Canonical 2025-11-25 has exactly 2 dashes, no third
        assertThat(VersionNegotiator.stripPatch("2025-11-25"))
            .isEqualTo("2025-11-25");
    }

    @Test
    void stripPatch_stripsFourthSegment() {
        // 2025-11-25-rc1 has 3 dashes — strip suffix
        assertThat(VersionNegotiator.stripPatch("2025-11-25-rc1"))
            .isEqualTo("2025-11-25");
    }

    @Test
    void stripPatch_nullReturnsNull() {
        assertThat(VersionNegotiator.stripPatch(null)).isNull();
    }
}