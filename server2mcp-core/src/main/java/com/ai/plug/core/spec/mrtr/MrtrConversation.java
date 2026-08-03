package com.ai.plug.core.spec.mrtr;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server-side state of a single Multi Round-Trip Request (MRTR) conversation.
 *
 * <p>A conversation accumulates:
 * <ul>
 *   <li>The most recent {@link MrtrTypes.InputRequiredResult} sent to the client;</li>
 *   <li>The chain of {@link MrtrTypes.InputResponses} received from the client;</li>
 *   <li>The opaque {@code requestState} token (also exposed to the client);</li>
 *   <li>The round counter (1-based) and creation timestamp;</li>
 *   <li>Optional user-defined {@code handlerSnapshot} that an
 *       {@link MrtrDriver}-style integration can serialize to resume
 *       long-running handlers across rounds.</li>
 * </ul>
 *
 * <p>The conversation is <b>immutable from the outside</b>; mutations go
 * through {@link MrtrSessionStore}, which produces a new instance on each
 * update.
 *
 * @author han
 * @time 2026/8/3
 */
public record MrtrConversation(
    String requestState,
    MrtrTypes.InputRequiredResult lastRequest,
    List<MrtrTypes.InputResponses> responseHistory,
    int round,
    Instant createdAt,
    Map<String, Object> handlerSnapshot
) {

    public MrtrConversation {
        if (requestState == null || requestState.isBlank()) {
            throw new IllegalArgumentException("requestState is required");
        }
        if (lastRequest == null) {
            throw new IllegalArgumentException("lastRequest is required");
        }
        if (round < 1) {
            throw new IllegalArgumentException("round must be >= 1");
        }
        responseHistory = responseHistory == null
            ? List.of() : List.copyOf(responseHistory);
        handlerSnapshot = handlerSnapshot == null
            ? Map.of() : Map.copyOf(handlerSnapshot);
    }

    /**
     * The very first round of a conversation — used by
     * {@link MrtrSessionStore#start}.
     */
    public static MrtrConversation first(String requestState,
                                          MrtrTypes.InputRequiredResult request,
                                          Map<String, Object> handlerSnapshot) {
        return new MrtrConversation(requestState, request,
            new ArrayList<>(), 1, Instant.now(), handlerSnapshot);
    }

    /**
     * Build the next-round conversation by appending a client response.
     * Internal: {@link MrtrSessionStore} calls this when the client retries.
     *
     * @param nextRequest the next {@code InputRequiredResult} the handler
     *                    produced after consuming the responses
     */
    public MrtrConversation next(MrtrTypes.InputResponses responses,
                                  MrtrTypes.InputRequiredResult nextRequest) {
        java.util.ArrayList<MrtrTypes.InputResponses> history =
            new java.util.ArrayList<>(this.responseHistory);
        history.add(responses);
        return new MrtrConversation(this.requestState, nextRequest,
            List.copyOf(history), this.round + 1, this.createdAt,
            this.handlerSnapshot);
    }

    /**
     * Whether the conversation has reached its terminal round — i.e. the
     * handler returned a non-MRTR result on the most recent invocation.
     * Use this in tests and metrics; not a hard invariant on the type
     * (the handler is responsible for ending the conversation via
     * {@link MrtrSessionStore#complete}).
     */
    public boolean isTerminal() {
        return lastRequest == null || responseHistory.size() >= round;
    }
}