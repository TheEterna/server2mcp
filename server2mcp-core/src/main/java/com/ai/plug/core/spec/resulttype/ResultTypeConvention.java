package com.ai.plug.core.spec.resulttype;

/**
 * Convention for MCP result types (protocol 2026-07-28 SEP-2322).
 * <p>
 * MCP 2026-07-28 mandates that every result carries a required {@code resultType}
 * field with one of two values:
 * <ul>
 *   <li>{@code "complete"} — ordinary result (the only kind this framework produces today)</li>
 *   <li>{@code "input_required"} — Multi Round-Trip Requests (MRTR) interim result,
 *       requiring the client to retry with {@code inputResponses}</li>
 * </ul>
 *
 * <h2>当前 SDK 状态（2.0）</h2>
 * MCP Java SDK 2.0 does <b>not</b> expose a {@code resultType} field on any
 * {@code McpSchema.Result} subtype (verified by grep over the shipped bytecode).
 * The {@code Result} interface itself is just {@code { Map<String,Object> meta() }}.
 *
 * <h2>本框架的角色</h2>
 * This class centralizes the protocol-level constants so that:
 * <ol>
 *   <li>User code can already reference {@link #COMPLETE} today without waiting
 *       for SDK support;</li>
 *   <li>The instant Java SDK 2.1+ adds the field, a single migration shim can
 *       route these constants through SDK builders — business code stays untouched.</li>
 * </ol>
 *
 * @author han
 * @time 2026/8/1 00:18
 */
public final class ResultTypeConvention {

    /** Ordinary result — every result this framework produces is complete. */
    public static final String COMPLETE = "complete";

    /**
     * Multi Round-Trip Requests interim result. {@code McpSchema.InputRequiredResult}
     * does not exist in MCP Java SDK 2.0; clients that need MRTR semantics should
     * hand-build the JSON-RPC payload using these string constants until SDK
     * exposure arrives.
     */
    public static final String INPUT_REQUIRED = "input_required";

    private ResultTypeConvention() {
    }

    /**
     * Assert that a value is a valid {@code resultType} literal. Defensive guard
     * for code that assembles wire payloads by hand before SDK support lands.
     *
     * @throws IllegalArgumentException if value is not {@link #COMPLETE} or
     *                                  {@link #INPUT_REQUIRED}
     */
    public static void validate(String value) {
        if (value == null) {
            throw new IllegalArgumentException("resultType must not be null");
        }
        if (!COMPLETE.equals(value) && !INPUT_REQUIRED.equals(value)) {
            throw new IllegalArgumentException(
                "resultType must be '" + COMPLETE + "' or '" + INPUT_REQUIRED + "', got: " + value);
        }
    }
}