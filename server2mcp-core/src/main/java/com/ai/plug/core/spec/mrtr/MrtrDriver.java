package com.ai.plug.core.spec.mrtr;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Helper for invoking {@link MrtrSessionStore} on behalf of a tool that
 * wants to participate in Multi Round-Trip Requests (MRTR).
 *
 * <p>This class is intentionally minimal: it provides the two endpoints
 * a tool handler needs — start a conversation (server returns its first
 * {@link MrtrTypes.InputRequiredResult}) and resume after the client
 * replies (server consumes the {@link MrtrTypes.InputResponses} and
 * either returns the next interim result or signals completion).
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   MrtrSessionStore store = new InMemoryMrtrSessionStore();
 *
 *   // Round 1: handler asks the client for an address.
 *   MrtrDriver.Started started = MrtrDriver.start(store,
 *       ctx -> {
 *           if (ctx.partialArgs().containsKey("address")) return MrtrDriver.done(order);
 *           return MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(List.of(
 *               MrtrTypes.ElicitationInputRequest.create("Need address",
 *                   Map.of("type", "object")))));
 *       },
 *       Map.of("item", "SKU-1"));
 *
 *   // Round 2: client retries with the address.
 *   MrtrDriver.Resumed resumed = MrtrDriver.resume(store,
 *       started.requestState(),
 *       responses -> responses.answers(),
 *       Map.of("address", "..."));
 *
 *   if (resumed.completed()) {
 *       // done — resumed.finalResult()
 *   }
 * }</pre>
 *
 * @author han
 * @time 2026/8/3
 */
public final class MrtrDriver {

    private MrtrDriver() {
    }

    /** Start a conversation: produce the first {@code InputRequiredResult}
     *  via {@code initialHandler}, store it, return the assigned
     *  {@code requestState} + the result. */
    public static Started start(MrtrSessionStore store,
                                 Function<RoundContext, Outcome> initialHandler,
                                 Map<String, Object> initialArgs) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(initialHandler, "initialHandler");
        RoundContext ctx = new RoundContext(initialArgs, Map.of(), 1);
        Outcome outcome = initialHandler.apply(ctx);
        if (outcome.isDone()) {
            // Handler decided to skip MRTR — caller should just return
            // outcome.finalResult() and never persist the session.
            return new Started(null, null, outcome.finalResult());
        }
        String token = UUID.randomUUID().toString();
        MrtrConversation seeded = MrtrConversation.first(
            token,
            outcome.nextRequest(),
            /* snapshot */ Map.of());
        store.start(seeded);
        return new Started(token, outcome.nextRequest(), null);
    }

    /** Resume a conversation: append the client's responses, give the
     *  handler access to accumulated state, and either advance or
     *  complete. */
    public static Resumed resume(MrtrSessionStore store,
                                  String requestState,
                                  MrtrTypes.InputResponses responses,
                                  Function<RoundContext, Outcome> handler,
                                  Map<String, Object> partialArgs) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(responses, "responses");
        Objects.requireNonNull(handler, "handler");
        var existing = store.get(requestState)
            .orElseThrow(() -> new IllegalStateException(
                "Unknown requestState: " + requestState
                    + " (session expired or never started)"));
        // Round-limit guard — abandon before incrementing so the next
        // call sees an empty store and the wrapper treats the state as
        // stale (cf. MrtrToolCallbackWrapper stale-token branch).
        int nextRound = existing.round() + 1;
        int maxRounds = MrtrSafetyLimits.maxRounds();
        if (nextRound > maxRounds) {
            store.abandon(requestState);
            throw new MrtrRoundLimitExceededException(requestState, maxRounds);
        }
        // Append responses to history; round counter advances.
        store.append(requestState, responses);
        // Merge all prior answers with the current round's so the handler
        // sees the full conversation context (not just the latest round).
        java.util.HashMap<String, Object> merged = new java.util.HashMap<>();
        for (MrtrTypes.InputResponses prior : store.get(requestState)
                .map(MrtrConversation::responseHistory).orElse(List.of())) {
            if (prior.answers() != null) merged.putAll(prior.answers());
        }
        if (responses.answers() != null) merged.putAll(responses.answers());
        RoundContext ctx = new RoundContext(
            partialArgs == null ? Map.of() : partialArgs,
            Map.copyOf(merged),
            nextRound);
        Outcome outcome = handler.apply(ctx);
        if (outcome.isDone()) {
            store.complete(requestState);
            return new Resumed(true, null, outcome.finalResult());
        }
        // Store the next-round interim result under the same token.
        MrtrConversation advanced = existing.next(responses, outcome.nextRequest());
        store.start(advanced); // store.start re-uses the existing token
        return new Resumed(false, outcome.nextRequest(), null);
    }

    /** Convenience: build a "next round" outcome. */
    public static Outcome nextRound(MrtrTypes.InputRequiredResult request) {
        return new Outcome(request, null);
    }

    /** Convenience: build a "done" outcome with a final result. */
    public static Outcome done(Object finalResult) {
        return new Outcome(null, finalResult);
    }

    /** Either: produce the next interim request OR signal completion. */
    public record Outcome(
        MrtrTypes.InputRequiredResult nextRequest,
        Object finalResult
    ) {
        public Outcome {
            if ((nextRequest == null) == (finalResult == null)) {
                throw new IllegalArgumentException(
                    "Outcome must carry exactly one of nextRequest / finalResult");
            }
        }
        public boolean isDone() { return finalResult != null; }
    }

    /** Per-round context passed to handlers: accumulated args + responses
     *  + the round number. */
    public record RoundContext(
        Map<String, Object> partialArgs,
        Map<String, Object> accumulatedResponses,
        int round
    ) {
        public RoundContext {
            partialArgs = partialArgs == null ? Map.of() : Map.copyOf(partialArgs);
            accumulatedResponses = accumulatedResponses == null
                ? Map.of() : Map.copyOf(accumulatedResponses);
        }
    }

    /** Return value of {@link #start}. */
    public record Started(
        String requestState,
        MrtrTypes.InputRequiredResult nextRequest,
        Object finalResult
    ) {
        public boolean isImmediate() { return nextRequest == null; }
    }

    /** Return value of {@link #resume}. */
    public record Resumed(
        boolean completed,
        MrtrTypes.InputRequiredResult nextRequest,
        Object finalResult
    ) {}
}