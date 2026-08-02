package com.ai.plug.core.spec.inspection;

import com.ai.plug.core.context.tool.IToolContext;
import com.ai.plug.common.utils.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * File writer for tool snapshot — dumps
 * {@link ToolsInspectionHelper#inspect(IToolContext)} output to a JSON file
 * for audit / debugging purposes.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   &#64;EventListener(ContextRefreshedEvent.class)
 *   public void snapshotTools() {
 *       Path file = ToolsSnapshotFileWriter.writeDefault(
 *           toolContext, Path.of("logs"));
 *       log.info("Tool snapshot: {}", file);
 *   }
 * }</pre>
 *
 * <p>File name includes an ISO-8601 timestamp suffix: {@code tools-2026-08-03T012345Z.json}.
 */
public final class ToolsSnapshotFileWriter {

    private ToolsSnapshotFileWriter() {
    }

    /**
     * Write the snapshot to the given directory using the default filename
     * (timestamped). Returns the path to the created file.
     *
     * @throws IOException if the directory cannot be created or the file
     *                     cannot be written
     */
    public static Path writeDefault(IToolContext toolContext, Path directory) throws IOException {
        if (toolContext == null) {
            throw new IllegalArgumentException("toolContext is required");
        }
        if (directory == null) {
            throw new IllegalArgumentException("directory is required");
        }
        Files.createDirectories(directory);
        String fileName = "tools-" + Instant.now().toString().replace(':', '-') + ".json";
        Path file = directory.resolve(fileName);
        return write(toolContext, file);
    }

    /**
     * Write the snapshot to a specific file path. Overwrites existing.
     */
    public static Path write(IToolContext toolContext, Path file) throws IOException {
        if (toolContext == null) {
            throw new IllegalArgumentException("toolContext is required");
        }
        if (file == null) {
            throw new IllegalArgumentException("file is required");
        }
        List<Map<String, Object>> report = ToolsInspectionHelper.inspect(toolContext);
        String json = JsonParser.getObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(report);
        Files.writeString(file, json);
        return file;
    }
}