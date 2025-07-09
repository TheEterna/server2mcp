package com.ai.plug.core.spec.utils.sampling;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;

/**
 * @author han
 * @time 2025/7/8 18:12
 */

public class McpSamplingFactory {

    public static McpSampling getSampling(Object exchange) {

        if (exchange instanceof McpSyncServerExchange syncExchange) {
            return new McpSyncSampling(syncExchange);
        } else if (exchange instanceof McpAsyncServerExchange asyncExchange) {
            return new McpAsyncSampling(asyncExchange);
        }

        throw new IllegalArgumentException("Unsupported exchange type: " + exchange.getClass().getName()
        + "exchange must be McpSyncServerExchange or McpAsyncServerExchange");


    }
}
