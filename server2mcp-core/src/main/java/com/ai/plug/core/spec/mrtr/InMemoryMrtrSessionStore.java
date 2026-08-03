package com.ai.plug.core.spec.mrtr;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link MrtrSessionStore}. Thread-safe via
 * {@link ConcurrentHashMap}; suitable for single-instance deployments.
 *
 * <p>Production deployments with multiple server replicas should swap
 * this for a distributed implementation (Redis, JDBC, etc.) using the
 * same {@link MrtrSessionStore} contract — the framework code that
 * uses the store is unaware of the backing technology.
 *
 * @author han
 * @time 2026/8/3
 */
public final class InMemoryMrtrSessionStore implements MrtrSessionStore {

    private final ConcurrentHashMap<String, MrtrConversation> sessions = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public String start(MrtrConversation conversation) {
        String token = conversation.requestState();
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString();
            // Reconstruct with the assigned token; the input conversation
            // is not mutated.
            conversation = new MrtrConversation(token,
                conversation.lastRequest(),
                conversation.responseHistory(),
                conversation.round(),
                conversation.createdAt(),
                conversation.handlerSnapshot());
        }
        sessions.put(token, conversation);
        return token;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<MrtrConversation> append(String requestState,
                                              MrtrTypes.InputResponses responses) {
        if (requestState == null) return Optional.empty();
        MrtrConversation existing = sessions.get(requestState);
        if (existing == null) return Optional.empty();
        // next() requires the next request to be set by the handler —
        // but for this store we accept that the handler is responsible
        // for storing its own continuation. The append here only
        // appends the response without advancing lastRequest; callers
        // should follow up with a start() of the next-round conversation.
        java.util.ArrayList<MrtrTypes.InputResponses> history =
            new java.util.ArrayList<>(existing.responseHistory());
        history.add(responses);
        MrtrConversation updated = new MrtrConversation(
            existing.requestState(),
            existing.lastRequest(),
            java.util.List.copyOf(history),
            existing.round() + 1,
            existing.createdAt(),
            existing.handlerSnapshot());
        sessions.put(requestState, updated);
        return Optional.of(updated);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<MrtrConversation> get(String requestState) {
        if (requestState == null) return Optional.empty();
        return Optional.ofNullable(sessions.get(requestState));
    }

    /** {@inheritDoc} */
    @Override
    public void complete(String requestState) {
        if (requestState != null) sessions.remove(requestState);
    }

    /** {@inheritDoc} */
    @Override
    public void abandon(String requestState) {
        if (requestState != null) sessions.remove(requestState);
    }

    /** {@inheritDoc} */
    @Override
    public int activeCount() {
        return sessions.size();
    }
}