package com.ai.plug.core.spec.utils.root;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * @author han
 * @time 2025/7/11 15:46
 */

public class McpAsyncRoot implements McpRoot {

    private final McpAsyncServerExchange exchange;

    public McpAsyncRoot(McpAsyncServerExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * 异步列出所有的根 async list all roots
     *
     * @return Mono<McpSchema.ListRootsResult>
     */
    @Override
    public Mono<McpSchema.ListRootsResult> listRootsAsync() {
        return exchange.listRoots();
    }

    /**
     * 同步列出所有的根 sync list all roots
     *
     * @return Mono<McpSchema.ListRootsResult>
     */
    @Override
    public McpSchema.ListRootsResult listRoots() {
        return this.listRootsAsync().block();
    }
}
