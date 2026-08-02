/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.core.spec.inspection;

import com.ai.plug.core.context.tool.IToolContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolsSnapshotFileWriterTest {

    @Test
    void write_toSpecificPath_createsFile(@TempDir Path tmp) throws Exception {
        IToolContext ctx = proxyCtx(Map.of());
        Path file = tmp.resolve("snapshot.json");
        Path written = ToolsSnapshotFileWriter.write(ctx, file);
        assertThat(written).isEqualTo(file);
        assertThat(Files.exists(file)).isTrue();
        String content = Files.readString(file);
        assertThat(content.replace(" ", "")).contains("[]"); // empty list (ignoring pretty spaces)
    }

    @Test
    void writeDefault_createsTimestampedFile(@TempDir Path tmp) throws Exception {
        IToolContext ctx = proxyCtx(Map.of());
        Path written = ToolsSnapshotFileWriter.writeDefault(ctx, tmp);
        assertThat(Files.exists(written)).isTrue();
        assertThat(written.getParent()).isEqualTo(tmp);
        assertThat(written.getFileName().toString())
            .startsWith("tools-")
            .endsWith(".json");
    }

    @Test
    void writeDefault_createsParentDirectory(@TempDir Path tmp) throws Exception {
        IToolContext ctx = proxyCtx(Map.of());
        Path nested = tmp.resolve("nested/sub");
        ToolsSnapshotFileWriter.writeDefault(ctx, nested);
        assertThat(Files.isDirectory(nested)).isTrue();
    }

    @Test
    void write_nullToolContextRejected(@TempDir Path tmp) {
        assertThatThrownBy(() -> ToolsSnapshotFileWriter.write(null, tmp.resolve("x.json")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void writeDefault_nullDirectoryRejected() {
        assertThatThrownBy(() -> ToolsSnapshotFileWriter.writeDefault(proxyCtx(Map.of()), null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roundTrip_prettyPrintedJson(@TempDir Path tmp) throws Exception {
        IToolContext ctx = proxyCtx(Map.of());
        Path file = tmp.resolve("snap.json");
        Path written = ToolsSnapshotFileWriter.write(ctx, file);
        String content = Files.readString(written);
        // Round trip: read back the JSON, must parse to a List
        var report = com.ai.plug.common.utils.JsonParser.getObjectMapper()
            .readValue(content, java.util.List.class);
        assertThat(report).isNotNull();
        // Empty context -> empty list
        assertThat(report).isEmpty();
    }

    private static IToolContext proxyCtx(Map<String, ?> snapshot) {
        return (IToolContext) Proxy.newProxyInstance(
            ToolsSnapshotFileWriterTest.class.getClassLoader(),
            new Class<?>[]{IToolContext.class},
            (proxy, method, args) -> {
                if ("getRawTools".equals(method.getName())) {
                    return new java.util.HashMap<>(snapshot);
                }
                return null;
            });
    }
}