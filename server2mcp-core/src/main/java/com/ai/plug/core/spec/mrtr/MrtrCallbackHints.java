package com.ai.plug.core.spec.mrtr;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * Helpers for plugging {@link MrtrDriver} into the existing tool callback
 * pipeline without forcing every integrator to learn the driver API.
 *
 * <p>The helpers are deliberately small and side-effect-bounded:
 * <ul>
 *   <li>{@link #resolveInputResponses} reads a {@code requestState} from a
 *       method's {@link MrtrInputResponses}-annotated argument and looks up
 *       the accumulated responses in the store;</li>
 *   <li>{@link #startSessionAfterInputRequired} inspects a tool's return
 *       value and, if it's an {@link MrtrTypes.InputRequiredResult},
 *       persists the session and returns the assigned
 *       {@code requestState} (or {@code null} for non-MRTR returns);</li>
 *   <li>{@link #completeIfTerminal} cleans up the session when the tool
 *       returns a non-MRTR result.</li>
 * </ul>
 *
 * <h2>用法（伪代码）</h2>
 * <pre>{@code
 *   // Inside your tool callback wrapper:
 *   Map<String, Object> responses = MrtrCallbackHints.resolveInputResponses(
 *       method, args, exchange.requestState(), store);
 *   if (responses != null) args[responsesIdx] = responses;
 *
 *   Object result = method.invoke(target, args);
 *
 *   String token = MrtrCallbackHints.startSessionAfterInputRequired(
 *       result, store);
 *   if (token == null) {
 *       // non-MRTR — caller may also want to complete a session in flight
 *       MrtrCallbackHints.completeIfTerminal(result, exchange.requestState(), store);
 *   } else {
 *       exchange.attachRequestState(token);
 *   }
 * }</pre>
 *
 * @author han
 * @time 2026/8/3
 */
public final class MrtrCallbackHints {

    private MrtrCallbackHints() {
    }

    /**
     * Find the index of the parameter annotated with
     * {@link MrtrInputResponses} in {@code methodParams}; if found, look up
     * the accumulated responses in {@code store} (keyed by
     * {@code requestState}) and return them. Returns {@code null} if:
     * <ul>
     *   <li>no annotated parameter exists;</li>
     *   <li>{@code requestState} is null/blank (caller never opted in);</li>
     *   <li>the store has no record for this {@code requestState};</li>
     *   <li>the conversation has no responses yet.</li>
     * </ul>
     *
     * @param methodParams reflective parameters of the tool method
     * @param requestState opaque token echoed from the client (may be null)
     * @param store        the MRTR session store
     */
    public static @Nullable Map<String, Object> resolveInputResponses(
        Parameter[] methodParams,
        @Nullable String requestState,
        MrtrSessionStore store) {

        int idx = findInputResponsesIndex(methodParams);
        if (idx < 0 || requestState == null || requestState.isBlank()) {
            return null;
        }
        return store.get(requestState)
            .map(MrtrConversation::responseHistory)
            .filter(h -> !h.isEmpty())
            .map(h -> h.get(h.size() - 1).answers())
            .orElse(null);
    }

    /**
     * If {@code result} is an {@link MrtrTypes.InputRequiredResult}, persist
     * a new MRTR session in {@code store} (or advance an existing one if
     * {@code requestState} is provided) and return the assigned
     * {@code requestState} token. Otherwise (handler returned a final
     * result) return {@code null}.
     *
     * <p>Pass {@code requestState} when the client retried with a prior
     * token — the helper will reuse it. Pass {@code null} for fresh
     * invocations.
     */
    public static @Nullable String startSessionAfterInputRequired(
        @Nullable Object result,
        @Nullable String requestState,
        MrtrSessionStore store) {

        if (!(result instanceof MrtrTypes.InputRequiredResult irr)) {
            return null;
        }
        String token = (requestState == null || requestState.isBlank())
            ? java.util.UUID.randomUUID().toString()
            : requestState;
        store.start(MrtrConversation.first(token, irr, Map.of()));
        return token;
    }

    /**
     * If {@code result} is NOT an {@code InputRequiredResult} AND
     * {@code requestState} is non-null, complete the session (removing it
     * from the store). Returns {@code true} if a session was completed.
     */
    public static boolean completeIfTerminal(
        @Nullable Object result,
        @Nullable String requestState,
        MrtrSessionStore store) {

        if (result instanceof MrtrTypes.InputRequiredResult) return false;
        if (requestState == null || requestState.isBlank()) return false;
        store.complete(requestState);
        return true;
    }

    /** Locate the index of the {@link MrtrInputResponses} parameter,
     *  or {@code -1} if absent. */
    public static int findInputResponsesIndex(Parameter[] methodParams) {
        for (int i = 0; i < methodParams.length; i++) {
            if (methodParams[i].isAnnotationPresent(MrtrInputResponses.class)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Marker annotation for tool method parameters that should receive the
     * accumulated client responses from the previous MRTR round. The
     * parameter type should be {@code Map<String, Object>} or a compatible
     * type.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MrtrInputResponses {
    }
}