package com.ai.plug.core.spec.utils.elicitation;

import com.ai.plug.core.spec.utils.logging.McpAsyncLogger;
import com.ai.plug.core.spec.utils.logging.McpLogger;
import com.ai.plug.core.spec.utils.logging.McpSyncLogger;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;

/**
 * @author han
 * @time 2025/7/4 17:55
 */

public class McpElicitationFactory {

    /**
     * 获取一个McpElicitation对象
     * @param exchange McpServerExchange对象
     * @return
     */
    public static McpElicitation getElicitation(Object exchange) {

        if (exchange instanceof McpSyncServerExchange) {
            return new McpSyncElicitation((McpSyncServerExchange) exchange);
        } else if (exchange instanceof McpAsyncServerExchange) {
            return new McpAsyncElicitation((McpAsyncServerExchange) exchange);
        }
        // 不可能的情况
        // Impossible situation
        throw new IllegalArgumentException("exchange must be McpSyncServerExchange or McpAsyncServerExchange");
    }
}
