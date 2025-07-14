package com.ai.plug.core.spec.utils.root;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;

/**
 * @author han
 * @time 2025/7/11 15:46
 */

public class McpRootFactory {

    public static McpRoot getRoot(Object exchange) {

        if (exchange instanceof McpSyncServerExchange syncExchange) {
            return new McpSyncRoot(syncExchange);
        } else if (exchange instanceof McpAsyncServerExchange asyncExchange) {
            return new McpAsyncRoot(asyncExchange);
        }

        throw new IllegalArgumentException("Unsupported exchange type: " + exchange.getClass().getName()
                + "exchange must be McpSyncServerExchange or McpAsyncServerExchange");

    }

}
