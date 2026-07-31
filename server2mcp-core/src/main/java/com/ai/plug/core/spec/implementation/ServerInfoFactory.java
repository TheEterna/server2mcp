package com.ai.plug.core.spec.implementation;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * Builder helper for {@link McpSchema.Implementation} — the SDK 2.0
 * server-identity record passed to {@code McpSyncSpecification.serverInfo(...)}.
 * <p>
 * SDK 2.0's {@code Implementation} extends protocol 2025-11-25's server identity
 * schema: name + version are mandatory; {@code title}, {@code description},
 * {@code icons} (SEP-973), and {@code websiteUrl} are optional.
 * <p>
 * This factory is a thin convenience over {@code Implementation.builder(name, version)}
 * that lets users compose rich server identity in a single fluent call.
 *
 * @author han
 * @time 2026/7/31 19:08
 */
public final class ServerInfoFactory {

    private ServerInfoFactory() {
    }

    public static McpSchema.Implementation create(String name, String version) {
        return McpSchema.Implementation.builder(name, version).build();
    }

    public static McpSchema.Implementation create(String name, String version, String title, String description) {
        return McpSchema.Implementation.builder(name, version)
                .title(title)
                .description(description)
                .build();
    }

    /**
     * Full server identity with icons (SEP-973) and websiteUrl. Icons are
     * applied in declaration order.
     */
    public static McpSchema.Implementation createFull(String name, String version, String title,
                                                      String description, List<McpSchema.Icon> icons,
                                                      String websiteUrl) {
        McpSchema.Implementation.Builder b = McpSchema.Implementation.builder(name, version)
                .title(title)
                .description(description);
        if (icons != null && !icons.isEmpty()) {
            b.icons(icons);
        }
        if (websiteUrl != null && !websiteUrl.isBlank()) {
            b.websiteUrl(websiteUrl);
        }
        return b.build();
    }

    /**
     * Build a single {@link McpSchema.Icon} from a {@code src[|mimeType[|sizes[|theme]]]}
     * pipe-delimited string (same convention as {@code @McpTool.icons()}).
     */
    public static McpSchema.Icon parseIcon(String entry) {
        if (entry == null || entry.isBlank()) {
            throw new IllegalArgumentException("icon src must not be blank");
        }
        String[] parts = entry.split("\\|", -1);
        String src = parts[0].trim();
        if (src.isEmpty()) {
            throw new IllegalArgumentException("icon src must not be blank");
        }
        McpSchema.Icon.Builder b = McpSchema.Icon.builder(src);
        if (parts.length > 1 && !parts[1].isBlank()) {
            b.mimeType(parts[1].trim());
        }
        if (parts.length > 2 && !parts[2].isBlank()) {
            b.sizes(List.of(parts[2].trim().split(",")));
        }
        if (parts.length > 3 && !parts[3].isBlank()) {
            b.theme(parts[3].trim());
        }
        return b.build();
    }
}