package com.ai.plug.core.spec.utils.logging;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;

/**
 * MCP 日志工厂
 * mcplog factory
 * @author han
 * @time 2025/6/24 14:06
 */

public class McpLoggerFactory {


    public static McpLogger getAsyncLogger(McpAsyncServer asyncServer, McpAsyncServerExchange exchange, Class<?> clazz) {
        return new McpAsyncLogger(asyncServer, exchange, clazz);
    }

    public static McpLogger getSyncLogger(McpSyncServer syncServer, McpSyncServerExchange exchange, Class<?> clazz) {
        return new McpSyncLogger(syncServer, exchange, clazz);
    }
    public static McpLogger getLogger(Object server, Object exchange, Class<?> clazz) {

        if (exchange instanceof McpSyncServerExchange) {
            return new McpSyncLogger(null, (McpSyncServerExchange) exchange, clazz);
        } else if (exchange instanceof McpAsyncServerExchange) {
            return new McpAsyncLogger(null, (McpAsyncServerExchange) exchange, clazz);
        }
        // 不可能的情况
        // Impossible situation
        throw new IllegalArgumentException("exchange must be McpSyncServerExchange or McpAsyncServerExchange");
    }
}
