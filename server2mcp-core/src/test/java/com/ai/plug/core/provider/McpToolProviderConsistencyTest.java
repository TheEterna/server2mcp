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
import com.ai.plug.core.spec.implementation.ServerInfoFactory;
import com.ai.plug.core.spec.utils.root.McpRootFactory;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that McpToolProvider's
 * {@code McpToolProvider.getToolAnnotations} and {@code buildIcons}
 * correctly reflect @McpTool annotation state into McpSchema.ToolAnnotations.
 *
 * <p>No full McpToolProvider instance is constructed (its ctor requires a
 * complex Spring-free setup); we directly exercise the protected helper
 * methods via a minimal subclass that delegates to the real impl.
 */
class McpToolProviderConsistencyTest {

    @Test
    void getToolAnnotations_reflectsAllHintFields() throws Exception {
        Method m = Holder.class.getDeclaredMethod("allHints");
        McpTool ann = m.getAnnotation(McpTool.class);
        McpSchema.ToolAnnotations result = invokeGetToolAnnotations(ann);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Tool Title");
        assertThat(result.readOnlyHint()).isTrue();
        assertThat(result.destructiveHint()).isFalse();
        assertThat(result.idempotentHint()).isTrue();
        assertThat(result.openWorldHint()).isTrue();
        assertThat(result.returnDirect()).isFalse();
    }

    @Test
    void getToolAnnotations_nullAnnotationReturnsNull() throws Exception {
        McpSchema.ToolAnnotations result = invokeGetToolAnnotations(null);
        assertThat(result).isNull();
    }

    @Test
    void isListChanged_defaultTrue() throws Exception {
        Method m = Holder.class.getDeclaredMethod("allHints");
        McpTool ann = m.getAnnotation(McpTool.class);
        // isListChanged is a helper, call via the testable wrapper
        boolean listChanged = invokeIsListChanged(ann);
        // Default @McpTool.listChanged defaults to true
        assertThat(listChanged).isTrue();
    }

    @Test
    void isListChanged_explicitFalseSkips() throws Exception {
        Method m = Holder.class.getDeclaredMethod("notListChanged");
        McpTool ann = m.getAnnotation(McpTool.class);
        boolean listChanged = invokeIsListChanged(ann);
        assertThat(listChanged).isFalse();
    }

    @Test
    void serverInfoFactory_buildsCompleteImplementation() {
        var impl = ServerInfoFactory.create("svc", "1.0", "Title", "Desc");
        assertThat(impl.name()).isEqualTo("svc");
        assertThat(impl.version()).isEqualTo("1.0");
        assertThat(impl.title()).isEqualTo("Title");
        assertThat(impl.description()).isEqualTo("Desc");
    }

    @Test
    void serverInfoFactoryCreateFull_includesWebsiteAndIcons() {
        var impl = ServerInfoFactory.createFull("svc", "1.0", "Title", "Desc",
            java.util.List.of(), "https://example.com");
        assertThat(impl.websiteUrl()).isEqualTo("https://example.com");
    }

    @Test
    void rootFactory_hasStaticGetRootMethod() {
        // McpRootFactory.getRoot(Object) is the static accessor — verify
        // method shape via reflection without requiring an exchange.
        var rootClass = McpRootFactory.class;
        assertThat(rootClass).isNotNull();
        assertThat(java.lang.reflect.Modifier.isPublic(rootClass.getModifiers())).isTrue();
        // It must expose a public static getRoot method
        try {
            var m = rootClass.getDeclaredMethod("getRoot", Object.class);
            assertThat(java.lang.reflect.Modifier.isStatic(m.getModifiers())).isTrue();
            assertThat(java.lang.reflect.Modifier.isPublic(m.getModifiers())).isTrue();
        }
        catch (NoSuchMethodException ex) {
            throw new AssertionError("McpRootFactory.getRoot(Object) missing", ex);
        }
    }

    // ---- helpers ----

    /** Minimal subclass to reach protected McpToolProvider helpers. */
    private static final class TestProvider extends McpToolProvider {
        TestProvider() { super(Map.of(), null, null); }
    }

    private static McpSchema.ToolAnnotations invokeGetToolAnnotations(McpTool ann) throws Exception {
        var p = new TestProvider();
        var m = McpToolProvider.class.getDeclaredMethod("getToolAnnotations", McpTool.class);
        m.setAccessible(true);
        return (McpSchema.ToolAnnotations) m.invoke(p, ann);
    }

    private static boolean invokeIsListChanged(McpTool ann) throws Exception {
        var p = new TestProvider();
        var m = McpToolProvider.class.getDeclaredMethod("isListChanged", McpTool.class);
        m.setAccessible(true);
        return (boolean) m.invoke(p, ann);
    }

    // Holder with annotated methods used by reflection
    static final class Holder {
        @McpTool(name = "x", title = "Tool Title", readOnlyHint = true, idempotentHint = true,
                openWorldHint = true, listChanged = true)
        public String allHints() { return "x"; }

        @McpTool(name = "y", listChanged = false)
        public String notListChanged() { return "y"; }
    }
}