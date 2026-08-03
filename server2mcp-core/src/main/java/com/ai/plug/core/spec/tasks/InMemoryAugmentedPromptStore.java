package com.ai.plug.core.spec.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default in-memory {@link AugmentedPromptStore}. Thread-safe via
 * per-task {@link CopyOnWriteArrayList} + {@link ConcurrentHashMap};
 * suitable for single-instance deployments.
 *
 * @author han
 * @time 2026/8/3
 */
public final class InMemoryAugmentedPromptStore implements AugmentedPromptStore {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<AugmentedPrompt>> byTaskId
        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> promptIdToTaskId = new ConcurrentHashMap<>();

    @Override
    public void add(AugmentedPrompt prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt is required");
        }
        byTaskId.computeIfAbsent(prompt.taskId(), k -> new CopyOnWriteArrayList<>())
            .add(prompt);
        promptIdToTaskId.put(prompt.promptId(), prompt.taskId());
    }

    @Override
    public List<AugmentedPrompt> list(String taskId) {
        if (taskId == null) return List.of();
        CopyOnWriteArrayList<AugmentedPrompt> bucket = byTaskId.get(taskId);
        return bucket == null ? List.of() : new ArrayList<>(bucket);
    }

    @Override
    public Optional<AugmentedPrompt> get(String promptId) {
        if (promptId == null) return Optional.empty();
        String taskId = promptIdToTaskId.get(promptId);
        if (taskId == null) return Optional.empty();
        return byTaskId.get(taskId).stream()
            .filter(p -> p.promptId().equals(promptId))
            .findFirst();
    }

    @Override
    public List<AugmentedPrompt> drain(String taskId) {
        if (taskId == null) return List.of();
        CopyOnWriteArrayList<AugmentedPrompt> bucket = byTaskId.remove(taskId);
        if (bucket == null) return List.of();
        // Clean up the id index
        for (AugmentedPrompt p : bucket) {
            promptIdToTaskId.remove(p.promptId());
        }
        return new ArrayList<>(bucket);
    }

    @Override
    public int activeCount() {
        return promptIdToTaskId.size();
    }
}