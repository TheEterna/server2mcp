package com.ai.plug.core.spec.utils.root;

import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * @author han
 * @time 2025/7/11 15:45
 */

public interface McpRoot {

    /**
     * 异步列出所有的根 async list all roots
     * @return Mono<McpSchema.ListRootsResult>
     */
    Mono<McpSchema.ListRootsResult> listRootsAsync();

    /**
     * 同步列出所有的根 sync list all roots
     * @return Mono<McpSchema.ListRootsResult>
     */
    McpSchema.ListRootsResult listRoots();

}
