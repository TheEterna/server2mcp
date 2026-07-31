package com.ai.plug.core.spec.utils.progress;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * 工具方法执行期间向客户端上报进度（protocol 2025-11-25 notifications/progress，请求作用域）。
 * <p>
 * 通过让工具方法参数列表声明 {@code McpProgress} 类型，本项目 callback 自动注入本接口实例；
 * 工具方法随后可按需多次调用 {@link #report(double)}、{@link #report(double, String)} 推送
 * 进度，无需关心 SDK 细节。SDK 2.0 由 {@code McpSyncServerExchange.progressNotification(…)}
 * 或 {@code McpAsyncServerExchange.progressNotification(…)}（reactor Mono）实现实际发送。
 *
 * <h2>progressToken 来源</h2>
 * SDK 协议规定 progressToken 由请求方（客户端）通过 {@code _meta.progressToken} 传递，
 * 本项目 callback 在收到工具调用时由 Spring AI 链路读出，注入到本接口实现内。无 progressToken
 * 的请求（client 不想收进度）会得到一个 {@link #isNoOp()} 为 {@code true} 的实例，所有
 * report 调用均为 no-op，避免污染协议。
 *
 * @author han
 * @time 2026/7/31 17:50
 */
public interface McpProgress {

    /**
     * 上报当前进度（不带 message）。
     *
     * @param progress 当前进度值（建议 0.0~1.0 之间或任意非负数值，具体语义由调用方约定）
     */
    void report(double progress);

    /**
     * 上报当前进度并附带给人看的状态消息（如「已处理 30%」）。
     */
    void report(double progress, String message);

    /**
     * @return 当请求未携带 progressToken 时返回 {@code true}（所有 report 为 no-op），
     *         否则返回 {@code false}。
     */
    boolean isNoOp();

    /**
     * @return 当前进度 token 的字符串表示；无 token 时返回 null。仅用于诊断日志，不要序列化。
     */
    String progressToken();

    /**
     * SDK 2.0 原生 {@code ProgressNotification}；仅在调用方需要直接序列化或走 SDK 全链路时使用，
     * 一般场景走 {@link #report(double)} 即可。
     */
    default McpSchema.ProgressNotification snapshot(double progress, @org.jspecify.annotations.Nullable String message) {
        // 仅 NoOpProgressToken 路径下不应到达这里；为契约清晰起见仍做防御。
        throw new IllegalStateException("snapshot() 不在 NoOpProgressToken 路径下调用；使用 isNoOp() 分流");
    }

}