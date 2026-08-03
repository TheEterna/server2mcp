package com.ai.plug.core.spec.mrtr;

import java.util.Optional;

/**
 * Server-side storage for Multi Round-Trip Request (MRTR) sessions.
 * <p>
 * When a tool returns {@link MrtrTypes.InputRequiredResult} the framework
 * may associate an opaque {@code requestState} token with the partial
 * conversation. Subsequent retries (the client re-invokes the tool with
 * an {@link MrtrTypes.InputResponses} envelope) look up the in-flight
 * session via {@code requestState} and resume.
 *
 * <p>This interface is intentionally transport-agnostic. The default
 * implementation is in-memory; production deployments may back this
 * with Redis, a JDBC table, or any TTL-evicting store.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #start} returns an opaque {@code requestState} and stores
 *       the initial {@link MrtrConversation};</li>
 *   <li>{@link #append} advances an existing conversation with the
 *       client's {@link MrtrTypes.InputResponses};</li>
 *   <li>{@link #get} retrieves the current state for resume;</li>
 *   <li>{@link #complete} or {@link #abandon} removes the session.</li>
 * </ol>
 *
 * @author han
 * @time 2026/8/3
 */
public interface MrtrSessionStore {

    /**
     * Begin a new MRTR conversation and return the assigned opaque
     * {@code requestState} token. The returned token must be sent to
     * the client as part of the {@link MrtrTypes.InputRequiredResult}
     * response; the client then echoes it back when retrying.
     *
     * @param conversation the initial conversation state
     * @return the assigned {@code requestState} token (non-null)
     */
    String start(MrtrConversation conversation);

    /**
     * Append the client's response to an existing conversation and
     * return the updated state. Returns empty if the {@code requestState}
     * has expired or was never issued.
     */
    Optional<MrtrConversation> append(String requestState,
                                       MrtrTypes.InputResponses responses);

    /**
     * Look up the current conversation state by {@code requestState}.
     */
    Optional<MrtrConversation> get(String requestState);

    /**
     * Mark the session complete and remove it from the store. Safe to
     * call on a non-existent session (no-op).
     */
    void complete(String requestState);

    /**
     * Mark the session abandoned (e.g. client gave up, or handler threw
     * unrecoverable) and remove it. Safe to call on a non-existent
     * session (no-op).
     */
    void abandon(String requestState);

    /**
     * @return number of live sessions in the store. Useful for tests
     *         and operational metrics.
     */
    int activeCount();
}