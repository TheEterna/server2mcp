package com.ai.plug.core.spec.tasks;

import com.ai.plug.common.utils.JsonParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Framework-layer HTTP endpoint for
 * {@code GET /mcp/tasks/&#123;id&#125;/augmented-prompts}.
 *
 * <p>SDK 2.0 has no JSON-RPC schema for
 * {@code tasks/augmented-prompt}, so the framework exposes it at the
 * HTTP layer. Downstream transports mount the contract directly.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var store = new InMemoryAugmentedPromptStore();
 *   var endpoint = new AugmentedPromptEndpoint(store);
 *   store.add(AugmentedPrompt.of("task-1", "assistant",
 *       "Halfway done; the answer will be 42."));
 *   String json = endpoint.handleListJson("task-1");
 * }</pre>
 *
 * @author han
 * @time 2026/8/3
 */
public final class AugmentedPromptEndpoint {

    private final AugmentedPromptStore store;

    public AugmentedPromptEndpoint(AugmentedPromptStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store is required");
        }
        this.store = store;
    }

    /**
     * List (without draining) all prompts for a task. Returns a map:
     * <pre>{@code
     *   { "taskId": "task-1", "count": 2, "prompts": [ ... ] }
     * }</pre>
     */
    public Map<String, Object> handleList(String taskId) {
        List<AugmentedPrompt> prompts = store.list(taskId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", taskId);
        body.put("count", prompts.size());
        body.put("prompts", prompts);
        return body;
    }

    /**
     * Drain (remove + return) all prompts for a task. Used when the
     * client has successfully consumed them.
     */
    public Map<String, Object> handleDrain(String taskId) {
        List<AugmentedPrompt> drained = store.drain(taskId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", taskId);
        body.put("drained", drained.size());
        body.put("prompts", drained);
        return body;
    }

    /** Convenience: serialize {@link #handleList} as JSON. */
    public String handleListJson(String taskId) throws java.io.IOException {
        return JsonParser.getObjectMapper().writeValueAsString(handleList(taskId));
    }

    /** Convenience: serialize {@link #handleDrain} as JSON. */
    public String handleDrainJson(String taskId) throws java.io.IOException {
        return JsonParser.getObjectMapper().writeValueAsString(handleDrain(taskId));
    }
}