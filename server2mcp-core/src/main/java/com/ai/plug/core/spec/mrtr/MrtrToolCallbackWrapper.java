package com.ai.plug.core.spec.mrtr;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Decorator that turns any existing sync tool callback into an MRTR-aware one
 * without touching its source code.
 *
 * <p>What it does (in order, per call):
 * <ol>
 *   <li><b>requestState extraction</b> — pulls the opaque token from
 *       {@code arguments["requestState"]} (convention: clients echo it back
 *       verbatim on retry);</li>
 *   <li><b>response injection</b> — finds the {@code @MrtrInputResponses}
 *       parameter on the tool's method and, if a prior session is in flight,
 *       injects the most recent answers into the args;</li>
 *   <li><b>delegate</b> — runs the inner callback exactly as before;</li>
 *   <li><b>session lifecycle</b> — if the returned {@code CallToolResult} is
 *       MRTR-shaped ({@code _meta.resultType == "input_required"}), persists
 *       the session in the store and writes the assigned
 *       {@code requestState} into {@code _meta.requestState}; otherwise
 *       completes any in-flight session.</li>
 * </ol>
 *
 * <p>The wrapper is fully opt-in: callers wrap their existing callback exactly
 * once at registration time. Non-MRTR tools are unaffected — they simply never
 * return an {@code input_required} result and have no annotated parameter, so
 * the wrapper's session lifecycle code path is a no-op for them.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   SyncMcpToolMethodCallback inner = SyncMcpToolMethodCallback.builder()...build();
 *   BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> wrapped =
 *       MrtrToolCallbackWrapper.wrap(inner, toolMethod, sessionStore);
 * }</pre>
 *
 * @author han
 * @time 2026/8/3
 */
public final class MrtrToolCallbackWrapper {

    private static final Logger logger = LoggerFactory.getLogger(MrtrToolCallbackWrapper.class);

    private MrtrToolCallbackWrapper() {
    }

    /**
     * Wrap {@code inner} so it transparently participates in MRTR multi-round
     * sessions. See class-level Javadoc for full behavior.
     *
     * @param inner    the existing sync tool callback (typically a
     *                 {@code SyncMcpToolMethodCallback})
     * @param method   the reflective {@link Method} the inner callback invokes
     *                 (used to locate the {@code @MrtrInputResponses} param)
     * @param store    the MRTR session store; may be null, in which case the
     *                 wrapper degrades to a pass-through (no session tracking)
     */
    public static BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> wrap(
        BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> inner,
        Method method,
        @Nullable MrtrSessionStore store) {

        if (store == null) {
            // No session store → MRTR is disabled; pass through unmodified.
            return inner;
        }

        Parameter[] params = method.getParameters();
        int responsesIdx = MrtrCallbackHints.findInputResponsesIndex(params);

        return (exchange, request) -> {
            // 1. Extract the echoed requestState from arguments (clients retry
            //    with the same token verbatim).
            Map<String, Object> args = request.arguments() != null
                ? new HashMap<>(request.arguments())
                : new HashMap<>();
            String echoedState = stringArg(args.remove("requestState"));

            // 2. Resolve accumulated responses (if a prior round is in flight)
            //    and inject them into the request arguments under the param's
            //    name so the existing callback's arg-builder finds them. If
            //    echoedState doesn't correspond to any in-flight session,
            //    invalidate it so the lifecycle branch generates a fresh token.
            if (responsesIdx >= 0 && echoedState != null) {
                Map<String, Object> responses = MrtrCallbackHints.resolveInputResponses(
                    params, echoedState, store);
                if (responses != null) {
                    String paramName = params[responsesIdx].getName();
                    args.put(paramName, responses);
                    logger.debug("MRTR injecting responses for {} param={} state={}",
                        method.getName(), paramName, echoedState);
                } else {
                    // Echoed state but no session → stale token, drop it.
                    echoedState = null;
                }
            }

            // 3. Delegate to the inner callback with the (possibly augmented)
            //    arguments. We rebuild the request because the SDK record's
            //    arguments() returns the original map.
            McpSchema.CallToolRequest augmented = rebuildRequest(request, args);

            McpSchema.CallToolResult result = inner.apply(exchange, augmented);

            // 4. Session lifecycle — based on the returned meta.
            Map<String, Object> meta = result.meta();
            boolean isInputRequired = isInputRequiredResult(meta);

            if (isInputRequired) {
                String token = (echoedState == null || echoedState.isBlank())
                    ? UUID.randomUUID().toString()
                    : echoedState;
                // Persist / advance session — the framework already wrote
                // inputRequests into meta, so we reuse it as the lastRequest.
                MrtrTypes.InputRequiredResult irr = readInputRequired(meta);
                if (irr != null) {
                    store.start(MrtrConversation.first(token, irr, Map.of()));
                }
                writeRequestState(result, token);
                logger.debug("MRTR started/advanced session token={} tool={}", token, method.getName());
            } else if (echoedState != null && !echoedState.isBlank()) {
                // Terminal result → close any in-flight session.
                store.complete(echoedState);
                logger.debug("MRTR completed session token={} tool={}", echoedState, method.getName());
            }

            return result;
        };
    }

    private static McpSchema.CallToolRequest rebuildRequest(
        McpSchema.CallToolRequest original, Map<String, Object> newArgs) {
        return new McpSchema.CallToolRequest(original.name(), newArgs, original.meta());
    }

    private static boolean isInputRequiredResult(@Nullable Map<String, Object> meta) {
        if (meta == null) return false;
        Object resultType = meta.get("resultType");
        return "input_required".equals(resultType);
    }

    /**
     * Read back the {@code InputRequiredResult} that the converter wrote into
     * {@code _meta.inputRequests} so we can store it as the session's
     * {@code lastRequest}. Best-effort: if any field is missing we return
     * {@code null} and skip persistence (the wrapper still writes
     * {@code requestState}, but no session is created).
     */
    @SuppressWarnings("unchecked")
    private static MrtrTypes.InputRequiredResult readInputRequired(@Nullable Map<String, Object> meta) {
        if (meta == null) return null;
        Object requestsObj = meta.get("inputRequests");
        if (!(requestsObj instanceof java.util.List<?> raw) || raw.isEmpty()) return null;
        try {
            java.util.List<MrtrTypes.InputRequest> requests =
                (java.util.List<MrtrTypes.InputRequest>) raw;
            Object stateObj = meta.get("requestState");
            String state = stateObj instanceof String s ? s : null;
            return MrtrTypes.InputRequiredResult.of(requests, state);
        } catch (ClassCastException ex) {
            logger.debug("inputRequests had unexpected element type: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Write {@code requestState} into the CallToolResult's _meta. Mutates the
     * SDK record's meta map in place — the SDK 2.0 record returns the
     * underlying map directly, so this propagates to the wire layer.
     */
    private static void writeRequestState(McpSchema.CallToolResult result, String token) {
        Map<String, Object> meta = result.meta();
        if (meta != null) {
            meta.put("requestState", token);
        }
    }

    private static @Nullable String stringArg(@Nullable Object value) {
        return (value instanceof String s && !s.isBlank()) ? s : null;
    }
}