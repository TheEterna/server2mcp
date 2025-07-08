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
        // 0. 完成组装 ElicitRequest 即可
        // 0 Complete the assembly of elicitrequest
        McpSchema.ElicitRequest.Builder builder =  McpSchema.ElicitRequest.builder();
        // 1.0 组装message部分
        // 1.0 assembling message part
        builder.message(message);
        // 2.0. 首先需要理清一下逻辑, 不能简单的把一个类的字段直接转成schema, 需要考虑一下上面有没有 需要忽略的字段
        // 2.0. First, clarify the logic. It's not possible to directly convert a class's fields into a schema without considering whether there are any fields that need to be ignored.
        // 2.1. 然后, 使用victools 生成 JSONSchema
        // 2.1. Then use victools generate JSONSchema
        ObjectNode jsonSchema = MCP_SCHEMA_GENERATOR.generateSchema(schema);
        // 2.2 convert ObjectNode to map
        // 2.2 进行转换
        Map<String, Object> mapSchema = GenSchemaUtils.objectNodeToMap(jsonSchema);
        builder.requestedSchema(mapSchema);
        // 3.0 进行构建获取 ElicitRequest
        // 3.0 Build to Obtain ElicitRequest
        McpSchema.ElicitRequest elicitRequest = builder.build();
        // 4.0 进行请求
        // 4.0 request
        return exchange.createElicitation(elicitRequest).block();
    }
    @Override
    public Mono<McpSchema.ElicitResult> elicitAsync(String message, Class<?> schema) {
        // 0. 完成组装 ElicitRequest 即可
        // 0 Complete the assembly of elicitrequest
        McpSchema.ElicitRequest.Builder builder =  McpSchema.ElicitRequest.builder();
        // 1.0 组装message部分
        // 1.0 assembling message part
        builder.message(message);
        // 2.0. 首先需要理清一下逻辑, 不能简单的把一个类的字段直接转成schema, 需要考虑一下上面有没有 需要忽略的字段
        // 2.0. First, clarify the logic. It's not possible to directly convert a class's fields into a schema without considering whether there are any fields that need to be ignored.
        // 2.1. 然后, 使用victools 生成 JSONSchema
        // 2.1. Then use victools generate JSONSchema
        ObjectNode jsonSchema = MCP_SCHEMA_GENERATOR.generateSchema(schema);
        // 2.2 convert ObjectNode to map
        // 2.2 进行转换
        Map<String, Object> mapSchema = GenSchemaUtils.objectNodeToMap(jsonSchema);
        builder.requestedSchema(mapSchema);
        // 3.0 进行构建获取 ElicitRequest
        // 3.0 Build to Obtain ElicitRequest
        McpSchema.ElicitRequest elicitRequest = builder.build();
        // 4.0 进行请求
        // 4.0 request
        return exchange.createElicitation(elicitRequest);
    }


}
