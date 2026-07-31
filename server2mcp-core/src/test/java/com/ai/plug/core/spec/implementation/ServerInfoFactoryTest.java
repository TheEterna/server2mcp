/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.implementation;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerInfoFactoryTest {

    @Test
    void create_minimal() {
        McpSchema.Implementation impl = ServerInfoFactory.create("svc", "1.0.0");
        assertThat(impl.name()).isEqualTo("svc");
        assertThat(impl.version()).isEqualTo("1.0.0");
        // optional fields remain at default (null for title, description)
    }

    @Test
    void create_withTitleAndDescription() {
        McpSchema.Implementation impl = ServerInfoFactory.create("svc", "2.1", "My Service", "Hello world");
        assertThat(impl.title()).isEqualTo("My Service");
        assertThat(impl.description()).isEqualTo("Hello world");
    }

    @Test
    void createFull_withIconsAndWebsite() {
        List<McpSchema.Icon> icons = List.of(
            ServerInfoFactory.parseIcon("data:image/svg+xml,abc|image/svg+xml|32x32|light"));
        McpSchema.Implementation impl = ServerInfoFactory.createFull(
            "svc", "3.0", "S", "D", icons, "https://example.com");
        assertThat(impl.title()).isEqualTo("S");
        assertThat(impl.icons()).hasSize(1);
        assertThat(impl.icons().get(0).src()).isEqualTo("data:image/svg+xml,abc");
        assertThat(impl.icons().get(0).mimeType()).isEqualTo("image/svg+xml");
        assertThat(impl.icons().get(0).sizes()).containsExactly("32x32");
        assertThat(impl.icons().get(0).theme()).isEqualTo("light");
        assertThat(impl.websiteUrl()).isEqualTo("https://example.com");
    }

    @Test
    void createFull_nullIconsAndWebsite() {
        McpSchema.Implementation impl = ServerInfoFactory.createFull("svc", "1", "T", "D", null, null);
        assertThat(impl.icons()).isNull();
        assertThat(impl.websiteUrl()).isNull();
    }

    @Test
    void createFull_emptyWebsiteUrlOmitted() {
        McpSchema.Implementation impl = ServerInfoFactory.createFull("svc", "1", "T", "D", null, "   ");
        assertThat(impl.websiteUrl()).isNull();
    }

    @Test
    void parseIcon_minimal() {
        McpSchema.Icon icon = ServerInfoFactory.parseIcon("https://cdn.example.com/x.png");
        assertThat(icon.src()).isEqualTo("https://cdn.example.com/x.png");
        assertThat(icon.mimeType()).isNull();
        assertThat(icon.sizes()).isNull();
        assertThat(icon.theme()).isNull();
    }

    @Test
    void parseIcon_fullFormat() {
        McpSchema.Icon icon = ServerInfoFactory.parseIcon(
            "https://example.com/icon.svg|image/svg+xml|16x16,32x32|dark");
        assertThat(icon.mimeType()).isEqualTo("image/svg+xml");
        assertThat(icon.sizes()).containsExactly("16x16", "32x32");
        assertThat(icon.theme()).isEqualTo("dark");
    }

    @Test
    void parseIcon_blankRejected() {
        assertThatThrownBy(() -> ServerInfoFactory.parseIcon(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServerInfoFactory.parseIcon("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }
}