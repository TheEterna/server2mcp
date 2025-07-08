package com.ai.plug.core.spec.utils.elicitation;

import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * 通过 exchange 向 客户端 发起elicitation 请求
 * @author han
 * @time 2025/7/4 16:04
 */

public interface McpElicitation {

    /**
     * 通过 exchange 向 客户端 发起elicitation 请求, 指定反序列化的 格式
     * @param message 请求的消息
     * @param schema 反序列化的格式
     * @return McpSchema.ElicitResult
     */
    McpSchema.ElicitResult elicit(String message, Class<?> schema);

    /**
     * 通过 exchange 向 客户端 发起elicitation 请求, 指定反序列化的 格式
     * @param message 请求的消息
     * @param schema 反序列化的格式
     * @return McpSchema.ElicitResult
     */
    Mono<McpSchema.ElicitResult> elicitAsync(String message, Class<?> schema);


}
