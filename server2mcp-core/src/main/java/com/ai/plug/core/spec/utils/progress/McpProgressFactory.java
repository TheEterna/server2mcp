package com.ai.plug.core.spec.utils.progress;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.util.annotation.Nullable;

/**
 * 根据同步/异步 exchange 派生 {@link McpProgress} 实例。
 * <p>
 * progressToken 优先从 {@code CallToolRequest.meta().progressToken} 读取（SDK 2.0 协议
 * 2025-11-25 规定 token 走请求的 _meta）。读不到时构造为 no-op 实例。
 * <p>
 * callback 注入时 {@link #getProgress(Object, McpSchema.CallToolRequest)} 根据 exchange 实际类型
 * 路由到同步/异步实现，屏蔽同步/异步差异。
 *
 * @author han
 * @time 2026/7/31 17:50
 */
public final class McpProgressFactory {

    private McpProgressFactory() {
    }

    public static McpProgress createSyncProgress(McpSyncServerExchange exchange,
                                                  @Nullable McpSchema.CallToolRequest request) {
        return new McpSyncProgress(exchange, extractToken(request));
    }

    public static McpProgress createAsyncProgress(McpAsyncServerExchange exchange,
                                                   @Nullable McpSchema.CallToolRequest request) {
        return new McpAsyncProgress(exchange, extractToken(request));
    }

    /**
     * callback 注入入口：按 exchange 实际类型分发到对应工厂。
     */
    public static McpProgress getProgress(Object exchange, @Nullable McpSchema.CallToolRequest request) {
        if (exchange instanceof McpSyncServerExchange sync) {
            return createSyncProgress(sync, request);
        }
        if (exchange instanceof McpAsyncServerExchange async) {
            return createAsyncProgress(async, request);
        }
        // 非 MCP exchange（理论上不应到达） → 返回 no-op 实例，避免 NPE
        return new McpProgress() {
            @Override
            public void report(double progress) {
            }

            @Override
            public void report(double progress, String message) {
            }

            @Override
            public boolean isNoOp() {
                return true;
            }

            @Override
            public String progressToken() {
                return null;
            }
        };
    }

    @Nullable
    private static Object extractToken(@Nullable McpSchema.CallToolRequest request) {
        if (request == null || request.meta() == null) {
            return null;
        }
        return request.meta().get("progressToken");
    }
}