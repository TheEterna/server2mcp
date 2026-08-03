package com.ai.plug.core.spec.jsonrpc;

import com.ai.plug.core.spec.change.NotificationsPollingEndpoint;
import com.ai.plug.core.spec.discover.DiscoverEndpoint;
import com.ai.plug.core.spec.mrtr.MrtrSessionStore;
import com.ai.plug.core.spec.mrtr.MrtrTypes;
import com.ai.plug.core.spec.tasks.AugmentedPromptEndpoint;
import com.ai.plug.core.spec.tasks.TaskStore;
import com.ai.plug.core.spec.tasks.TaskTypes.TaskHandle;
import com.ai.plug.core.spec.tasks.TasksEndpoint;

import java.util.Map;

/**
 * One-stop registry that wires the protocol-2026-07-28 JSON-RPC method
 * names to the framework's existing endpoint implementations. Each
 * registration takes raw {@code params} (a JSON object) and returns the
 * JSON-RPC {@code result} object — no SDK types cross the wire.
 *
 * <h2>Registered methods (8)</h2>
 * <ul>
 *   <li>{@code server/discover}</li>
 *   <li>{@code tasks/create}</li>
 *   <li>{@code tasks/get}</li>
 *   <li>{@code tasks/list}</li>
 *   <li>{@code tasks/cancel}</li>
 *   <li>{@code tasks/augmented-prompt}</li>
 *   <li>{@code subscriptions/listen}</li>
 *   <li>{@code input_required/respond} (MRTR envelope)</li>
 * </ul>
 *
 * @author han
 * @time 2026/8/3
 */
public final class JsonRpcRoutes {

    private JsonRpcRoutes() {
    }

    /** Register all protocol-2026-07-28 routes against {@code router}. */
    public static JsonRpcRouter registerAll(
        JsonRpcRouter router,
        DiscoverEndpoint discoverEndpoint,
        TaskStore taskStore,
        TasksEndpoint tasksEndpoint,
        AugmentedPromptEndpoint augmentedPromptEndpoint,
        NotificationsPollingEndpoint notificationsEndpoint,
        MrtrSessionStore mrtrStore) {

        // 1. server/discover — capability negotiation. We emit the full
        //    server capabilities payload (which the endpoint already
        //    packages against the protocol-2026-07-28 wire schema).
        router.register("server/discover", params -> discoverEndpoint.handle());

        // 2. tasks/create — submit an async task. The endpoint only
        //    exposes get/list/cancel over HTTP; creation is a
        //    TaskStore.register call (idempotent at the wire layer).
        router.register("tasks/create", params -> {
            String title = (String) params.getOrDefault("title", "untitled");
            String taskId = "task-" + System.currentTimeMillis() + "-"
                + Long.toHexString(System.nanoTime());
            taskStore.register(TaskHandle.of(taskId));
            return Map.of(
                "taskId", taskId,
                "title", title,
                "status", "running");
        });

        // 3. tasks/get — query one task
        router.register("tasks/get", params -> {
            String taskId = (String) params.get("taskId");
            return tasksEndpoint.handleGet(taskId);
        });

        // 4. tasks/list — enumerate all tasks
        router.register("tasks/list", params -> tasksEndpoint.handleList());

        // 5. tasks/cancel — cancel one task
        router.register("tasks/cancel", params -> {
            String taskId = (String) params.get("taskId");
            String reason = (String) params.get("reason");
            return tasksEndpoint.handleCancel(taskId, reason);
        });

        // 6. tasks/augmented-prompt — fetch augmented prompt for a task
        router.register("tasks/augmented-prompt", params -> {
            String taskId = (String) params.get("taskId");
            boolean drain = Boolean.TRUE.equals(params.get("drain"));
            return drain
                ? augmentedPromptEndpoint.handleDrain(taskId)
                : augmentedPromptEndpoint.handleList(taskId);
        });

        // 7. subscriptions/listen — return pending notifications (poll mode).
        //    Note: the real protocol-2026-07-28 wire is SSE long-poll;
        //    polling here is the SDK-2.0-compatible fallback. The SDK ≥ 3.0.0
        //    upgrade will replace this handler with a streaming response.
        router.register("subscriptions/listen", params -> {
            Object sinceObj = params.get("since");
            long since = sinceObj instanceof Number n ? n.longValue() : -1;
            return notificationsEndpoint.handlePoll(since);
        });

        // 8. input_required/respond — MRTR envelope: append client answers
        //    to an in-flight session and return the next interim result.
        router.register("input_required/respond", params -> {
            String requestState = (String) params.get("requestState");
            @SuppressWarnings("unchecked")
            Map<String, Object> answers = (Map<String, Object>) params.getOrDefault("answers", Map.of());
            if (requestState != null && !requestState.isBlank()) {
                mrtrStore.append(requestState, MrtrTypes.InputResponses.of(answers));
            }
            return Map.of(
                "status", "accepted",
                "requestState", requestState,
                "answers", answers);
        });

        return router;
    }
}