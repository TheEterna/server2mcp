package com.ai.plug.core.spec.mrtr;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * End-to-end tests for {@link MrtrToolCallbackWrapper}: an existing tool
 * callback is wrapped and the resulting decorator transparently handles
 * MRTR lifecycle — response injection, session start, requestState round-trip,
 * and session completion on terminal results.
 *
 * @author han
 * @time 2026/8/3
 */
class MrtrToolCallbackWrapperTest {

    /**
     * Test fixture: a tool whose method signature carries an
     * {@code @MrtrInputResponses} parameter. The method returns either a
     * plain {@link Address} (terminal) or an {@link MrtrTypes.InputRequiredResult}
     * (needs more info from the client).
     */
    static class AddressTool {
        public record Address(String street, String city) {}

        public Object submit(@MrtrCallbackHints.MrtrInputResponses Map<String, Object> prior, String street, String city) {
            if (prior == null || !prior.containsKey("street")) {
                // Round 1 — need to ask the client for the address.
                Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "street", Map.of("type", "string"),
                        "city", Map.of("type", "string")
                    )
                );
                return MrtrTypes.InputRequiredResult.of(
                    List.of(MrtrTypes.ElicitationInputRequest.create("Provide shipping address", schema)),
                    null
                );
            }
            // Round 2+ — accept the previously-elicitied answers.
            return new Address((String) prior.get("street"), (String) prior.get("city"));
        }
    }

    /**
     * Inner-callback stub that mimics what {@code SyncMcpToolMethodCallback}
     * does: looks at the method's raw return value, detects
     * {@link MrtrTypes.InputRequiredResult}, and produces a CallToolResult
     * whose _meta carries the MRTR fields. For terminal results, the stub
     * just returns the JSON of the result.
     *
     * <p>The wrapper injects accumulated responses under the param name
     * ({@code "prior"} in our test fixture), which is exactly what
     * {@code SyncMcpToolMethodCallback.buildArgs(...)} does when binding
     * arguments to parameters — so this stub mirrors that wiring.
     */
    private static final BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> FAKE_INNER =
        (exchange, request) -> {
            Object methodResult = invokeRaw(request);
            Map<String, Object> meta = new HashMap<>();
            if (methodResult instanceof MrtrTypes.InputRequiredResult irr) {
                meta.put("resultType", "input_required");
                meta.put("inputRequests", irr.inputRequests());
                return McpSchema.CallToolResult.builder()
                    .addTextContent("input_required")
                    .isError(false)
                    .meta(meta)
                    .build();
            }
            return McpSchema.CallToolResult.builder()
                .addTextContent("done")
                .isError(false)
                .meta(meta)
                .build();
        };

    /** Synthetic "method body" — branches on whether the wrapper injected
     *  accumulated responses (under the {@code prior} param name). */
    private static Object invokeRaw(McpSchema.CallToolRequest request) {
        Map<String, Object> args = request.arguments();
        if (args == null) return null;
        Object prior = args.get("prior");
        if (prior == null) {
            // Round 1 — no accumulated responses → request more info.
            Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                    "street", Map.of("type", "string"),
                    "city", Map.of("type", "string")
                )
            );
            return MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("Provide shipping address", schema)),
                null
            );
        }
        // Round 2+ — wrapper injected the responses; build a terminal result.
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = (Map<String, Object>) prior;
        return new AddressTool.Address((String) answers.get("street"), (String) answers.get("city"));
    }

    @Test
    void wrapper_disabledWhenStoreIsNull() throws Exception {
        // null store → wrapper is a pass-through.
        Method m = AddressTool.class.getMethod("submit", Map.class, String.class, String.class);
        BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> wrapped =
            MrtrToolCallbackWrapper.wrap(FAKE_INNER, m, null);

        McpSchema.CallToolResult result = wrapped.apply(mock(McpSyncServerExchange.class),
            new McpSchema.CallToolRequest("submit", Map.of("street", "x", "city", "y"), null));

        assertThat(result.meta()).doesNotContainKey("requestState");
        assertThat(result.isError()).isFalse();
    }

    @Test
    void wrapper_firstCall_returnsInputRequired_andStartsSession() throws Exception {
        InMemoryMrtrSessionStore store = new InMemoryMrtrSessionStore();
        Method m = AddressTool.class.getMethod("submit", Map.class, String.class, String.class);
        BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> wrapped =
            MrtrToolCallbackWrapper.wrap(FAKE_INNER, m, store);

        // Round 1: no prior responses, no echoed state → expect InputRequired + new token.
        McpSchema.CallToolResult result = wrapped.apply(mock(McpSyncServerExchange.class),
            new McpSchema.CallToolRequest("submit", Map.of(), null));

        assertThat(result.meta()).containsEntry("resultType", "input_required");
        assertThat(result.meta().get("requestState")).isNotNull();
        String token = (String) result.meta().get("requestState");
        assertThat(store.get(token)).isPresent();
        assertThat(store.get(token).get().lastRequest().resultType()).isEqualTo("input_required");
    }

    @Test
    void wrapper_retryWithEchoedState_injectsResponses_andCompletesSession() throws Exception {
        InMemoryMrtrSessionStore store = new InMemoryMrtrSessionStore();
        Method m = AddressTool.class.getMethod("submit", Map.class, String.class, String.class);
        BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> wrapped =
            MrtrToolCallbackWrapper.wrap(FAKE_INNER, m, store);

        // Round 1 — start session.
        McpSchema.CallToolResult r1 = wrapped.apply(mock(McpSyncServerExchange.class),
            new McpSchema.CallToolRequest("submit", Map.of(), null));
        String token = (String) r1.meta().get("requestState");
        // Simulate the client appending responses to the session.
        MrtrTypes.InputResponses responses = MrtrTypes.InputResponses.of(Map.of(
            "street", "123 Main",
            "city", "Springfield"
        ));
        store.append(token, responses);

        // Round 2 — client retries with echoed state. The wrapper injects
        // the accumulated answers into args; the inner callback returns a
        // terminal Address → wrapper completes the session.
        Map<String, Object> retryArgs = new HashMap<>();
        retryArgs.put("requestState", token);
        McpSchema.CallToolResult r2 = wrapped.apply(mock(McpSyncServerExchange.class),
            new McpSchema.CallToolRequest("submit", retryArgs, null));

        assertThat(r2.isError()).isFalse();
        assertThat(r2.meta()).doesNotContainKey("resultType");
        assertThat(store.get(token)).isEmpty(); // session completed
    }

    @Test
    void wrapper_terminalResult_doesNotStartSession() throws Exception {
        InMemoryMrtrSessionStore store = new InMemoryMrtrSessionStore();
        Method m = AddressTool.class.getMethod("submit", Map.class, String.class, String.class);
        // Inner callback that always returns a plain terminal result (no
        // InputRequiredResult). Used to verify the wrapper is a no-op on
        // non-MRTR tools.
        BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> plainInner =
            (exchange, request) -> McpSchema.CallToolResult.builder()
                .addTextContent("done").isError(false)
                .meta(new HashMap<>()).build();
        BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> wrapped =
            MrtrToolCallbackWrapper.wrap(plainInner, m, store);

        McpSchema.CallToolResult result = wrapped.apply(mock(McpSyncServerExchange.class),
            new McpSchema.CallToolRequest("submit", Map.of("street", "x", "city", "y"), null));

        assertThat(result.meta()).doesNotContainKey("requestState");
        assertThat(store.activeCount()).isZero();
    }

    @Test
    void wrapper_retryWithUnknownState_doesNotInjectResponses() throws Exception {
        InMemoryMrtrSessionStore store = new InMemoryMrtrSessionStore();
        Method m = AddressTool.class.getMethod("submit", Map.class, String.class, String.class);
        BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> wrapped =
            MrtrToolCallbackWrapper.wrap(FAKE_INNER, m, store);

        // Client echoes a state that doesn't exist in the store.
        McpSchema.CallToolResult result = wrapped.apply(mock(McpSyncServerExchange.class),
            new McpSchema.CallToolRequest("submit",
                Map.of("requestState", "ghost-token"),
                null));

        // The wrapper falls back to "no responses" → inner returns InputRequired
        // (because no street/city in args) → new session started under a fresh token.
        assertThat(result.meta()).containsEntry("resultType", "input_required");
        String newToken = (String) result.meta().get("requestState");
        assertThat(newToken).isNotEqualTo("ghost-token");
        assertThat(store.get(newToken)).isPresent();
    }
}