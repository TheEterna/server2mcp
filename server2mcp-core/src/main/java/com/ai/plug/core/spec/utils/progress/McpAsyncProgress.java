package com.ai.plug.core.spec.utils.progress;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

/**
 * 异步版 {@link McpProgress}，通过 {@code McpAsyncServerExchange.progressNotification} 发
 * （返回 {@link Mono}）。{@link #report(double)} 内部 subscribe；上游链式工具方法通常不
 * 直接调本接口，而是经 callback 处理 subscribe。
 *
 * @author han
 * @time 2026/7/31 17:50
 */
public class McpAsyncProgress implements McpProgress {

    private final McpAsyncServerExchange exchange;

    @Nullable
    private final Object progressToken;

    private final boolean noOp;

    public McpAsyncProgress(McpAsyncServerExchange exchange, @Nullable Object progressToken) {
        this.exchange = exchange;
        this.progressToken = progressToken;
        this.noOp = progressToken == null;
    }

    @Override
    public void report(double progress) {
        if (noOp) {
            return;
        }
        exchange.progressNotification(McpSchema.ProgressNotification.builder(progressToken, progress).build())
                .subscribe();
    }

    @Override
    public void report(double progress, String message) {
        if (noOp) {
            return;
        }
        exchange.progressNotification(
                McpSchema.ProgressNotification.builder(progressToken, progress).message(message).build())
                .subscribe();
    }

    @Override
    public boolean isNoOp() {
        return noOp;
    }

    @Override
    @Nullable
    public String progressToken() {
        return progressToken == null ? null : progressToken.toString();
    }

    /**
     * 异步订阅入口：返回 {@link Mono} 而非 fire-and-forget 的 report，便于上游链式组合。
     */
    public Mono<Void> progressNotification(double progress, @Nullable String message) {
        if (noOp) {
            return Mono.empty();
        }
        return exchange.progressNotification(
                McpSchema.ProgressNotification.builder(progressToken, progress).message(message).build());
    }
}