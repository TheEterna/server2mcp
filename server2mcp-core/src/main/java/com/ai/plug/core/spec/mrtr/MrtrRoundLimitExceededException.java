package com.ai.plug.core.spec.mrtr;

/**
 * Thrown by {@link MrtrDriver#resume} when a conversation exceeds
 * {@link MrtrSafetyLimits#maxRounds()}. The session is abandoned (removed
 * from the store) before the exception is raised, so the next call with
 * the same {@code requestState} will see an "unknown token" error and
 * can start a fresh session.
 *
 * <p>Tools should catch this and translate to a user-friendly error
 * (e.g. "Too many follow-up questions; please contact support").
 *
 * @author han
 * @time 2026/8/3
 */
public class MrtrRoundLimitExceededException extends RuntimeException {

    private final String requestState;
    private final int limit;

    public MrtrRoundLimitExceededException(String requestState, int limit) {
        super("MRTR round limit exceeded for session " + requestState
            + " (limit=" + limit + "). Session abandoned.");
        this.requestState = requestState;
        this.limit = limit;
    }

    public String requestState() {
        return requestState;
    }

    public int limit() {
        return limit;
    }
}