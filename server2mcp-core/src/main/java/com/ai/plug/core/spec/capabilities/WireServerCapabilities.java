package com.ai.plug.core.spec.capabilities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Protocol-2026-07-28 {@code ServerCapabilities} wire schema — a 1:1 mirror
 * of the protocol's advertised fields, independent of any SDK record type.
 *
 * <p>Why a separate type: Java MCP SDK 2.0's
 * {@code io.modelcontextprotocol.spec.McpSchema.ServerCapabilities} record
 * was frozen against the older protocol (2025-11-25) and does not carry
 * 2026-07-28-only fields like {@code tools.subscription},
 * {@code completions.listChanged}, or the
 * {@code experimental.io.modelcontextprotocol/tasks} extension. Per the
 * board's 2026-08-03 13:16 authorising statement, we emit the
 * protocol-compliant shape ourselves rather than wait for SDK ≥ 3.0.0.
 *
 * <p>This type is intentionally non-final on field structure (uses
 * {@code Map<String,Object>} for {@code experimental}) so future
 * protocol revisions can be absorbed by adding entries rather than
 * breaking the record signature.
 *
 * @author han
 * @time 2026/8/3
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WireServerCapabilities(
    @JsonProperty("tools") Tools tools,
    @JsonProperty("resources") Resources resources,
    @JsonProperty("prompts") Prompts prompts,
    @JsonProperty("completions") Completions completions,
    @JsonProperty("logging") Map<String, Object> logging,
    @JsonProperty("experimental") Map<String, Object> experimental
) {

    /** Factory: builds the canonical 2026-07-28 capabilities with every
     *  field at its most permissive setting (clients can opt out per
     *  call via the request envelope). */
    public static WireServerCapabilities full() {
        return new WireServerCapabilities(
            new Tools(true, true),                    // listChanged + subscription
            new Resources(true, true),                // subscribe + listChanged
            new Prompts(true),                        // listChanged
            new Completions(true),                    // listChanged (2026-07-28)
            Map.of(),                                 // logging
            Map.of("io.modelcontextprotocol/tasks",  // experimental
                Map.of("subscribe", true))
        );
    }

    /** {@code tools} sub-shape: {@code listChanged} (legacy) +
     *  {@code subscription} (2026-07-28). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Tools(
        @JsonProperty("listChanged") Boolean listChanged,
        @JsonProperty("subscription") Boolean subscription
    ) {}

    /** {@code resources} sub-shape: subscribe + listChanged. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Resources(
        @JsonProperty("subscribe") Boolean subscribe,
        @JsonProperty("listChanged") Boolean listChanged
    ) {}

    /** {@code prompts} sub-shape: listChanged. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Prompts(
        @JsonProperty("listChanged") Boolean listChanged
    ) {}

    /** {@code completions} sub-shape: listChanged (new in 2026-07-28). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Completions(
        @JsonProperty("listChanged") Boolean listChanged
    ) {}
}