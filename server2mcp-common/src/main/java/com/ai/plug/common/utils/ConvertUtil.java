package com.ai.plug.common.utils;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author han
 * @time 2025/6/17 11:04
 */

public class ConvertUtil {

    public static McpServerFeatures.AsyncResourceSpecification resourceConvertAysnc(McpServerFeatures.SyncResourceSpecification resource) {
        return resource == null ? null : new McpServerFeatures.AsyncResourceSpecification(resource.resource(), (exchange, req) -> {
            return Mono.fromCallable(() -> {
                return (McpSchema.ReadResourceResult)resource.readHandler().apply(new McpSyncServerExchange(exchange), req);
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

}
