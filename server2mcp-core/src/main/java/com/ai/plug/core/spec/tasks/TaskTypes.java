package com.ai.plug.core.spec.tasks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Tasks extension (MCP protocol 2026-07-28 SEP-2663, namespace
 * {@code io.modelcontextprotocol/tasks}).
 * <p>
 * The Tasks extension moved out of the protocol core into a first-class
 * extension. The redesigned pattern:
 * <ul>
 *   <li>Servers may return task handles <b>unsolicited</b> — no per-request opt-in required;</li>
 *   <li>Clients poll via {@code tasks/get} to retrieve status / result;</li>
 *   <li>Clients send input via {@code tasks/update} (replacing the blocking
 *       {@code tasks/result} method);</li>
 *   <li>{@code tasks/list} was removed entirely.</li>
 * </ul>
 *
 * <h2>当前 SDK 状态（2.0）</h2>
 * MCP Java SDK 2.0 ships <b>no</b> Tasks schema (verified by grep — no
 * {@code TaskHandle} / {@code TaskStatus} / {@code tasks/get} anywhere in
 * shipped bytecode). This package therefore owns the entire wire schema.
 *
 * <h2>本框架的角色</h2>
 * Owns the data model, validation, and wire serialization for the three
 * Tasks RPC payloads. Server-side integrator assembles these when building
 * custom MCP servers; the JSON-RPC routing itself is the transport's job.
 *
 * @author han
 * @time 2026/8/1 02:02
 */
public final class TaskTypes {

    private TaskTypes() {
    }

    /**
     * Lifecycle states a task transitions through.
     * <p>
     * Jackson serialization: enum constant names are Java-style UPPER_CASE;
     * the wire protocol uses lowercase (per protocol 2026-07-28 SEP-2663),
     * so each constant carries an explicit {@link JsonProperty} alias.
     */
    public enum Status {
        /** Task accepted, not yet running. */
        @JsonProperty("pending") PENDING,
        /** Task is actively running; result not yet available. */
        @JsonProperty("running") RUNNING,
        /** Task completed; {@link TaskStatus#result} carries the value. */
        @JsonProperty("completed") COMPLETED,
        /** Task failed; {@link TaskStatus#error} carries the error code + message. */
        @JsonProperty("failed") FAILED,
        /** Task cancelled (client-initiated or server-side timeout). */
        @JsonProperty("cancelled") CANCELLED
    }

    /**
     * Opaque handle minted by the server. The protocol recommends UUID or
     * similar — see the integration guide for naming conventions.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskHandle(String taskId, Map<String, Object> meta) {

        public TaskHandle {
            if (taskId == null || taskId.isBlank()) {
                throw new IllegalArgumentException("taskId is required");
            }
        }

        public static TaskHandle of(String taskId) {
            return new TaskHandle(taskId, null);
        }

        public static TaskHandle of(String taskId, Map<String, Object> meta) {
            return new TaskHandle(taskId, meta);
        }
    }

    /**
     * Response to {@code tasks/get}. Carries the current {@link Status},
     * optional progress hint, and the terminal result/error when applicable.
     * For long-running tasks, this is the payload clients receive while
     * polling until the status transitions to {@link Status#COMPLETED} or
     * {@link Status#FAILED}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskStatus(Status status,
                             Object result,
                             TaskError error,
                             Double progress,
                             String message,
                             Map<String, Object> meta) {

        public TaskStatus {
            if (status == null) {
                throw new IllegalArgumentException("status is required");
            }
            // Terminal states must carry result XOR error, never both, never neither.
            boolean terminal = status == Status.COMPLETED || status == Status.FAILED;
            if (terminal && result == null && error == null) {
                throw new IllegalArgumentException(
                    "terminal status (" + status + ") must carry either result or error");
            }
            if (result != null && error != null) {
                throw new IllegalArgumentException("status cannot carry both result and error");
            }
        }

        public static TaskStatus pending() {
            return new TaskStatus(Status.PENDING, null, null, null, null, null);
        }

        public static TaskStatus running(Double progress, String message) {
            return new TaskStatus(Status.RUNNING, null, null, progress, message, null);
        }

        public static TaskStatus completed(Object result) {
            return new TaskStatus(Status.COMPLETED, result, null, 1.0, null, null);
        }

        public static TaskStatus failed(int code, String message) {
            return new TaskStatus(Status.FAILED, null, new TaskError(code, message, null), null, message, null);
        }

        public static TaskStatus cancelled(String reason) {
            return new TaskStatus(Status.CANCELLED, null, null, null, reason, null);
        }
    }

    /**
     * Error payload for failed tasks. Mirrors JSON-RPC error shape (code +
     * message + optional data), enabling clients to surface failures uniformly
     * with transport-level errors.
     *
     * <p>Error code range (protocol 2026-07-28 changelog):
     * <ul>
     *   <li>-32000..-32019: implementation-defined (grandfathered)</li>
     *   <li>-32020..-32099: <b>reserved for MCP spec</b> — these are
     *       canonical codes like -32020 (HeaderMismatch), -32021
     *       (MissingRequiredClientCapability), -32022 (UnsupportedProtocolVersion)</li>
     * </ul>
     * This constructor accepts any code (JSON-RPC allows arbitrary ints in
     * the server-error range) but provides a static helper
     * {@link #reserved(int, String)} for emitting a reserved code.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskError(int code, String message, Map<String, Object> data) {

        /** Lowest reserved code per protocol 2026-07-28. */
        public static final int RESERVED_CODE_MIN = -32099;
        /** Highest reserved code per protocol 2026-07-28. */
        public static final int RESERVED_CODE_MAX = -32020;

        public TaskError {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("error message is required");
            }
        }

        public static TaskError of(int code, String message) {
            return new TaskError(code, message, null);
        }

        /**
         * Construct a TaskError with a reserved-range code (-32099 to -32020).
         * Throws IllegalArgumentException if the code is outside the reserved
         * range — call sites can use {@link #of(int, String)} for arbitrary
         * codes instead.
         */
        public static TaskError reserved(int code, String message) {
            // Valid range: MIN <= code <= MAX, i.e. -32099 <= code <= -32020.
            // code below MIN or above MAX is out of range.
            if (code < RESERVED_CODE_MIN || code > RESERVED_CODE_MAX) {
                throw new IllegalArgumentException(
                    "code " + code + " is outside the reserved range ["
                        + RESERVED_CODE_MIN + ".." + RESERVED_CODE_MAX + "]");
            }
            return new TaskError(code, message, null);
        }

        /** @return true if this error's code is in the protocol-reserved range. */
        public boolean isReservedCode() {
            return code >= RESERVED_CODE_MIN && code <= RESERVED_CODE_MAX;
        }
    }

    /**
     * Client → server input payload for {@code tasks/update}. Replaces the
     * blocking {@code tasks/result} method. The {@code input} is opaque to
     * the framework; the integrator's task implementation decides how to
     * interpret it (e.g. pause/resume signals, additional parameters).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskUpdate(String taskId, Object input, Map<String, Object> meta) {

        public TaskUpdate {
            if (taskId == null || taskId.isBlank()) {
                throw new IllegalArgumentException("taskId is required");
            }
        }

        public static TaskUpdate of(String taskId, Object input) {
            return new TaskUpdate(taskId, input, null);
        }
    }
}