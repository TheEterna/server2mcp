package com.ai.plug.core.spec.tasks;

import java.util.List;
import java.util.Optional;

/**
 * Server-side storage for augmented prompts attached to running tasks.
 *
 * <p>Long-running tasks may emit prompts that the client should append to
 * its LLM context window (protocol 2026-07-28 SEP-2663
 * {@code tasks/augmented-prompt}). The store keeps them until the client
 * drains them via {@code GET /mcp/tasks/&#123;id&#125;/augmented-prompts}.
 *
 * <p>Production deployments may back this with Redis / JDBC; the default
 * in-memory implementation is at {@link InMemoryAugmentedPromptStore}.
 *
 * @author han
 * @time 2026/8/3
 */
public interface AugmentedPromptStore {

    /** Append a new prompt to a task's queue. */
    void add(AugmentedPrompt prompt);

    /** List all prompts for a given task, oldest first. */
    List<AugmentedPrompt> list(String taskId);

    /** Get a single prompt by id. */
    Optional<AugmentedPrompt> get(String promptId);

    /**
     * Drain (remove and return) all prompts for a task — used by the
     * HTTP endpoint when the client successfully polled.
     */
    List<AugmentedPrompt> drain(String taskId);

    /** Total number of prompts buffered across all tasks. */
    int activeCount();
}