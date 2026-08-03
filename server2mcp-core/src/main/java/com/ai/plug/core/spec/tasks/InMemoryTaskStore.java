package com.ai.plug.core.spec.tasks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory {@link TaskStore}. Thread-safe via
 * {@link ConcurrentHashMap}; suitable for single-instance deployments.
 *
 * <p>Production deployments with multiple server replicas should swap
 * this for a distributed implementation (Redis, JDBC, etc.) using the
 * same {@link TaskStore} contract.
 *
 * @author han
 * @time 2026/8/3
 */
public final class InMemoryTaskStore implements TaskStore {

    private final ConcurrentHashMap<String, TaskTypes.TaskStatus> tasks = new ConcurrentHashMap<>();

    @Override
    public TaskTypes.TaskStatus register(TaskTypes.TaskHandle handle) {
        if (handle == null) {
            throw new IllegalArgumentException("handle is required");
        }
        // Initial status: RUNNING with progress=0; user tools can call
        // update() later to advance progress, complete, or fail.
        TaskTypes.TaskStatus initial = new TaskTypes.TaskStatus(
            TaskTypes.Status.RUNNING,
            null,
            null,
            0.0,
            "registered",
            handle.meta());
        tasks.put(handle.taskId(), initial);
        return initial;
    }

    @Override
    public Optional<TaskTypes.TaskStatus> get(String taskId) {
        if (taskId == null) return Optional.empty();
        return Optional.ofNullable(tasks.get(taskId));
    }

    @Override
    public List<TaskTypes.TaskStatus> list() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public Optional<TaskTypes.TaskStatus> update(String taskId, TaskTypes.TaskStatus status) {
        if (taskId == null || status == null) return Optional.empty();
        TaskTypes.TaskStatus existing = tasks.get(taskId);
        if (existing == null) return Optional.empty();
        tasks.put(taskId, status);
        return Optional.of(status);
    }

    @Override
    public boolean cancel(String taskId, String reason) {
        if (taskId == null) return false;
        TaskTypes.TaskStatus existing = tasks.get(taskId);
        if (existing == null) return false;
        if (existing.status() == TaskTypes.Status.CANCELLED) return true;
        tasks.put(taskId, new TaskTypes.TaskStatus(
            TaskTypes.Status.CANCELLED,
            existing.result(),
            existing.error(),
            existing.progress(),
            reason == null ? "cancelled" : reason,
            existing.meta()));
        return true;
    }

    @Override
    public int activeCount() {
        return tasks.size();
    }
}