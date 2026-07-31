package com.ai.plug.core.spec.mrtr;

import com.ai.plug.core.spec.resulttype.ResultTypeConvention;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Multi Round-Trip Requests (MRTR) wire-format types — MCP protocol 2026-07-28 SEP-2322.
 * <p>
 * SDK 2.0 ships <b>no</b> MRTR schema (verified by grep over shipped bytecode —
 * {@code InputRequiredResult} / {@code inputRequests} / {@code inputResponses} are all
 * absent). This package therefore owns the entire wire schema and the
 * serialization helpers, so user code can emit MRTR-compliant payloads today.
 *
 * <h2>协议定义（来自 changelog）</h2>
 * <ul>
 *   <li>{@code InputRequiredResult} — server 返回的 interim result，携带
 *       {@code resultType: "input_required"}（与 {@link ResultTypeConvention#INPUT_REQUIRED}）；
 *       含 {@code inputRequests} 字段，承载 server 需要 client 补充的信息请求；</li>
 *   <li>{@code inputResponses} — client 重试原始请求时附上的回应（与 {@code inputRequests} 一一对应）；
 *       server 据此恢复处理。</li>
 * </ul>
 *
 * <h2>三种内建 InputRequest 类型</h2>
 * <ul>
 *   <li>{@link ElicitationInputRequest} — server 需要 client 弹窗收集的 elicitation；</li>
 *   <li>{@link SamplingInputRequest} — server 需要 client 调用 LLM 的 sampling；</li>
 *   <li>{@link RootsInputRequest} — server 需要 client 的 roots 列表（取代已弃用的
 *       Roots 特性；MRTR 模式下保留读取能力）；</li>
 *   <li>通用 {@link JsonSchemaInputRequest} — 任意 JSON Schema 表单（向后兼容）。</li>
 * </ul>
 *
 * <p>所有 wire 字段命名严格遵循 protocol 2026-07-28 changelog 与 JSON-RPC 命名风格。
 *
 * @author han
 * @time 2026/8/1 02:02
 */
public final class MrtrTypes {

    private MrtrTypes() {
    }

    /**
     * Server-issued interim result: "I need more information before I can answer."
     * Wire JSON shape:
     * <pre>{@code
     * {
     *   "resultType": "input_required",
     *   "inputRequests": [
     *     { "kind": "elicitation", "message": "...", "schema": { ... } },
     *     { "kind": "sampling",    "message": "...", "maxTokens": 1024, "messages": [...] }
     *   ],
     *   "requestState": "server-side correlation token (optional, opaque)",
     *   "meta": { ... }
     * }
     * }</pre>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InputRequiredResult(
        String resultType,
        List<InputRequest> inputRequests,
        String requestState,
        Map<String, Object> meta
    ) {
        public InputRequiredResult {
            // Defense in depth: ResultTypeConvention validates the literal;
            // we re-check at construction so the record can't be built wrong.
            ResultTypeConvention.validate(resultType);
            if (resultType == null || !ResultTypeConvention.INPUT_REQUIRED.equals(resultType)) {
                throw new IllegalArgumentException(
                    "InputRequiredResult.resultType must be '" + ResultTypeConvention.INPUT_REQUIRED + "'");
            }
            if (inputRequests == null || inputRequests.isEmpty()) {
                throw new IllegalArgumentException("inputRequests must contain at least one item");
            }
        }

        public static InputRequiredResult of(List<InputRequest> requests, String requestState) {
            return new InputRequiredResult(ResultTypeConvention.INPUT_REQUIRED, requests, requestState, null);
        }

        public static InputRequiredResult of(List<InputRequest> requests) {
            return of(requests, null);
        }
    }

    /**
     * Marker interface for server-initiated input requests. Discriminated by
     * {@link #kind()} on the wire — every implementation carries a literal
     * {@code kind} field that the client uses to route the request to the right
     * handler.
     */
    public sealed interface InputRequest
        permits ElicitationInputRequest, SamplingInputRequest, RootsInputRequest, JsonSchemaInputRequest {

        /**
         * @return wire literal discriminator. One of {@code elicitation},
         *         {@code sampling}, {@code roots}, or a custom user-defined string.
         */
        String kind();
    }

    /**
     * Elicitation input request — server wants the client to show a form and
     * collect user input. {@code schema} is a JSON Schema (Draft 2020-12) the
     * client renders in its UI.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ElicitationInputRequest(String kind, String message,
                                            Map<String, Object> schema) implements InputRequest {

        public ElicitationInputRequest {
            if (!"elicitation".equals(kind)) {
                throw new IllegalArgumentException("kind must be 'elicitation', got: " + kind);
            }
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("message is required for elicitation");
            }
            if (schema == null) {
                throw new IllegalArgumentException("schema is required for elicitation");
            }
        }

        public static ElicitationInputRequest create(String message, Map<String, Object> schema) {
            return new ElicitationInputRequest("elicitation", message, schema);
        }
    }

    /**
     * Sampling input request — server wants the client to invoke an LLM. Mirrors
     * the legacy {@code sampling/createMessage} schema but used through MRTR
     * pattern.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SamplingInputRequest(String kind, String message, Integer maxTokens,
                                        List<Map<String, Object>> messages) implements InputRequest {

        public SamplingInputRequest {
            if (!"sampling".equals(kind)) {
                throw new IllegalArgumentException("kind must be 'sampling', got: " + kind);
            }
            if (messages == null || messages.isEmpty()) {
                throw new IllegalArgumentException("messages must contain at least one item");
            }
        }

        public static SamplingInputRequest create(List<Map<String, Object>> messages, Integer maxTokens) {
            return new SamplingInputRequest("sampling", "sampling requested", maxTokens, messages);
        }
    }

    /**
     * Roots input request — server wants the client's root list (used by
     * permission-bound tools). Mirrors the legacy {@code roots/list} but through
     * MRTR.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RootsInputRequest(String kind) implements InputRequest {

        public RootsInputRequest {
            if (!"roots".equals(kind)) {
                throw new IllegalArgumentException("kind must be 'roots', got: " + kind);
            }
        }

        public static RootsInputRequest create() {
            return new RootsInputRequest("roots");
        }
    }

    /**
     * Free-form input request carrying an arbitrary JSON schema. Useful when
     * the server wants to elicit data that doesn't fit the three canonical kinds.
     * The {@code kind} discriminator can be any non-empty string the integrator
     * defines and matches client-side.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record JsonSchemaInputRequest(String kind, String message,
                                          Map<String, Object> schema) implements InputRequest {

        public JsonSchemaInputRequest {
            if (kind == null || kind.isBlank()) {
                throw new IllegalArgumentException("kind is required for custom schema requests");
            }
            if ("elicitation".equals(kind) || "sampling".equals(kind) || "roots".equals(kind)) {
                throw new IllegalArgumentException(
                    "kind '" + kind + "' is reserved; use the dedicated InputRequest subtype instead");
            }
        }

        public static JsonSchemaInputRequest create(String kind, String message, Map<String, Object> schema) {
            return new JsonSchemaInputRequest(kind, message, schema);
        }
    }

    /**
     * Client response carrying answers for one or more {@link InputRequest}s. The
     * client retries the original request with this envelope attached under
     * {@code inputResponses}.
     * <p>
     * {@code answers} maps {@code kind} discriminator → user-supplied answer.
     * For elicitation this is the form's JSON object; for sampling this is the
     * model's reply; for roots this is the array of file URIs.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InputResponses(Map<String, Object> answers, Map<String, Object> meta) {

        public InputResponses {
            if (answers == null || answers.isEmpty()) {
                throw new IllegalArgumentException("answers must contain at least one entry");
            }
        }

        public static InputResponses of(Map<String, Object> answers) {
            return new InputResponses(answers, null);
        }
    }
}