package com.ai.plug.core.spec.utils.root;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * @author han
 * @time 2025/7/11 15:46
 */

public class McpSyncRoot implements McpRoot {

    private final McpSyncServerExchange exchange;

    public McpSyncRoot(McpSyncServerExchange exchange) {
        this.exchange = exchange;
    }/**
     * 异步列出所有的根 async list all roots
     *
     * @return Mono<McpSchema.ListRootsResult>
     */
    @Override
    public Mono<McpSchema.ListRootsResult> listRootsAsync() {
        return Mono.justOrEmpty(this.listRoots());
    }

    /**
     * 同步列出所有的根 sync list all roots
     *
     * @return Mono<McpSchema.ListRootsResult>
     */
    @Override
    public McpSchema.ListRootsResult listRoots() {

        return exchange.listRoots();
    }
}
