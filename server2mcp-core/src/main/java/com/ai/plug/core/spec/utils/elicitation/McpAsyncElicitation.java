package com.ai.plug.core.spec.utils.elicitation;

import com.ai.plug.core.utils.GenSchemaUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.ai.plug.core.utils.GenSchemaUtils.MCP_SCHEMA_GENERATOR;

/**
 * @author han
 * @time 2025/7/4 17:40
 */

public class McpAsyncElicitation implements McpElicitation{
    private final McpAsyncServerExchange exchange;

    public McpAsyncElicitation(McpAsyncServerExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * 通过 exchange 向 客户端 发起 elicitation 请求, 指定反序列化的 格式
     *
     * @param message 请求的消息
     * @param schema  反序列化的格式
     * @return McpSchema.ElicitResult
     */
    @Override
    public McpSchema.ElicitResult elicit(String message, Class<?> schema) {
        // return as POJO
        // 以实体类形式返回
        return this.elicitAsync(message, schema).block();
    }

    @Override
    public Mono<McpSchema.ElicitResult> elicitAsync(String message, Class<?> schema) {
        // build request
        McpSchema.ElicitRequest elicitRequest = buildElicitationRequest(message, schema);
        // 4.0 进行请求
        // 4.0 request
        return exchange.createElicitation(elicitRequest);
    }


}
