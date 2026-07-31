package com.ai.plug.core.spec.utils.progress;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;

/**
 * 同步版 {@link McpProgress}，通过 {@code McpSyncServerExchange.progressNotification} 发。
 * 当请求未带 progressToken 时构造为 no-op，所有 report 调用直接吞掉。
 *
 * @author han
 * @time 2026/7/31 17:50
 */
public class McpSyncProgress implements McpProgress {

    private final McpSyncServerExchange exchange;

    @Nullable
    private final Object progressToken;

    private final boolean noOp;

    public McpSyncProgress(McpSyncServerExchange exchange, @Nullable Object progressToken) {
        this.exchange = exchange;
        this.progressToken = progressToken;
        this.noOp = progressToken == null;
    }

    @Override
    public void report(double progress) {
        if (noOp) {
            return;
        }
        exchange.progressNotification(McpSchema.ProgressNotification.builder(progressToken, progress).build());
    }

    @Override
    public void report(double progress, String message) {
        if (noOp) {
            return;
        }
        exchange.progressNotification(
                McpSchema.ProgressNotification.builder(progressToken, progress).message(message).build());
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
}