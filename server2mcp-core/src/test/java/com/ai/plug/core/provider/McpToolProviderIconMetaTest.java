/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.provider;

import com.ai.plug.core.annotation.McpTool;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link McpToolProvider#buildIcons(McpTool)} and
 * {@link McpToolProvider#buildMeta(McpTool)} — the Tool.icons / Tool.meta
 * population introduced for MCP protocol 2025-11-25 (SEP-973).
 */
class McpToolProviderIconMetaTest {

    private final McpToolProvider provider = new McpToolProvider(
        java.util.Map.of(), null, null);

    // ---- icons ----

    @Test
    void buildIcons_emptyArrayReturnsNull() throws Exception {
        // empty array -> null so we don't emit a Tool.icons = [] list which
        // protocol parsers may interpret as "icons exist but none provided".
        McpTool ann = annotation(EmptyIconsHolder.class, "m");
        assertThat(provider.buildIcons(ann)).isNull();
    }

    @Test
    void buildIcons_blankEntriesSkipped() throws Exception {
        McpTool ann = annotation(BlankAndValidIconsHolder.class, "m");
        List<McpSchema.Icon> icons = provider.buildIcons(ann);
        assertThat(icons).hasSize(1);
        McpSchema.Icon only = icons.get(0);
        assertThat(only.src()).isEqualTo("data:image/svg+xml,...");
        assertThat(only.mimeType()).isEqualTo("image/svg+xml");
        assertThat(only.sizes()).containsExactly("16x16", "32x32");
        assertThat(only.theme()).isEqualTo("dark");
    }

    @Test
    void buildIcons_allBlankReturnsNull() throws Exception {
        McpTool ann = annotation(AllBlankIconsHolder.class, "m");
        assertThat(provider.buildIcons(ann)).isNull();
    }

    @Test
    void buildIcons_nullAnnotationReturnsNull() {
        assertThat(provider.buildIcons(null)).isNull();
    }

    // ---- meta ----

    @Test
    void buildMeta_validJsonParses() throws Exception {
        McpTool ann = annotation(MetaJsonHolder.class, "m");
        Map<String, Object> meta = provider.buildMeta(ann);
        assertThat(meta).containsEntry("vendor", "acme").containsEntry("version", 1);
    }

    @Test
    void buildMeta_blankReturnsNull() throws Exception {
        McpTool ann = annotation(MetaBlankHolder.class, "m");
        assertThat(provider.buildMeta(ann)).isNull();
    }

    @Test
    void buildMeta_invalidJsonThrows() throws Exception {
        McpTool ann = annotation(MetaBadHolder.class, "m");
        assertThatThrownBy(() -> provider.buildMeta(ann))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("@McpTool.metaJson");
    }

    @Test
    void buildMeta_nullAnnotationReturnsNull() {
        assertThat(provider.buildMeta(null)).isNull();
    }

    // ---- helpers ----

    private static McpTool annotation(Class<?> holder, String methodName) throws NoSuchMethodException {
        Method m = holder.getDeclaredMethod(methodName);
        return m.getAnnotation(McpTool.class);
    }

    static class EmptyIconsHolder {
        @McpTool(name = "x", icons = {})
        public void m() {
        }
    }

    static class BlankAndValidIconsHolder {
        @McpTool(name = "x", icons = {
            "   ",
            "data:image/svg+xml,...|image/svg+xml|16x16,32x32|dark"
        })
        public void m() {
        }
    }

    static class AllBlankIconsHolder {
        @McpTool(name = "x", icons = { "", "  " })
        public void m() {
        }
    }

    static class MetaJsonHolder {
        @McpTool(name = "x", metaJson = "{\"vendor\":\"acme\",\"version\":1}")
        public void m() {
        }
    }

    static class MetaBlankHolder {
        @McpTool(name = "x", metaJson = "   ")
        public void m() {
        }
    }

    static class MetaBadHolder {
        @McpTool(name = "x", metaJson = "not-json")
        public void m() {
        }
    }
}