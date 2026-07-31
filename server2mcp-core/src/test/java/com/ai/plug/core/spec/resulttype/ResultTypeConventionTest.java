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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTypeConventionTest {

    @Test
    void constantsHaveExpectedValues() {
        assertThat(ResultTypeConvention.COMPLETE).isEqualTo("complete");
        assertThat(ResultTypeConvention.INPUT_REQUIRED).isEqualTo("input_required");
    }

    @Test
    void validate_acceptsBothValidValues() {
        ResultTypeConvention.validate(ResultTypeConvention.COMPLETE);
        ResultTypeConvention.validate(ResultTypeConvention.INPUT_REQUIRED);
    }

    @Test
    void validate_rejectsNull() {
        assertThatThrownBy(() -> ResultTypeConvention.validate(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    void validate_rejectsUnknown() {
        assertThatThrownBy(() -> ResultTypeConvention.validate("partial"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("partial");
    }

    @Test
    void validate_rejectsEmpty() {
        assertThatThrownBy(() -> ResultTypeConvention.validate(""))
            .isInstanceOf(IllegalArgumentException.class);
    }
}