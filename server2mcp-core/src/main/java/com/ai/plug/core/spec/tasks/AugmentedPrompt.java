package com.ai.plug.core.spec.tasks;

import java.time.Instant;
import java.util.Map;

/**
 * A single augmented prompt attached to a task — protocol 2026-07-28
 * SEP-2663 {@code tasks/augmented-prompt}.
 *
 * <p>When a long-running task is in flight, the server may emit prompts
 * that the client should append to its LLM context window — for example,
 * a partial-progress snapshot, a clarifying question, or a hint about
 * the next steps. The client polls
 * {@code GET /mcp/tasks/&#123;id&#125;/augmented-prompts} to drain these.
 *
 * <p>SDK 2.0 has no schema for this RPC. This framework record provides
 * the wire shape so user code can attach prompts today and clients can
 * poll them via the framework-layer HTTP endpoint. SDK ≥ 3.0.0 upgrades
 * to a native JSON-RPC route with zero business-code change.
 *
 * @author han
 * @time 2026/8/3
 */
public record AugmentedPrompt(
    String promptId,
    String taskId,
    String role,
    String content,
    Instant timestamp,
    Map<String, Object> meta
) {

    public AugmentedPrompt {
        if (promptId == null || promptId.isBlank()) {
            throw new IllegalArgumentException("promptId is required");
        }
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId is required");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    /** Convenience factory with auto-generated promptId and timestamp. */
    public static AugmentedPrompt of(String taskId, String role, String content) {
        return new AugmentedPrompt(
            java.util.UUID.randomUUID().toString(),
            taskId, role, content, Instant.now(), null);
    }

    /** Convenience factory with explicit meta. */
    public static AugmentedPrompt of(String taskId, String role, String content,
                                       Map<String, Object> meta) {
        return new AugmentedPrompt(
            java.util.UUID.randomUUID().toString(),
            taskId, role, content, Instant.now(), meta);
    }
}