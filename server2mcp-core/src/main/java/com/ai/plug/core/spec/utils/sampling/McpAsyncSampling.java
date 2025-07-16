package com.ai.plug.core.spec.utils.sampling;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author han
 * @time 2025/7/8 18:08
 */

public non-sealed class McpAsyncSampling implements McpSampling{

    private final McpAsyncServerExchange exchange;

    public McpAsyncSampling(McpAsyncServerExchange exchange) {
        this.exchange = exchange;
    }
    /**
     * 向客户端发送同步采样请求 Send synchronous sampling request to the client
     *
     * @param messages   消息列表 message list
     * @param models     待选模型列表(仅供客户端参考) List of models to be selected (for client reference only)
     * @param llmOptions 模型参数 model parameter
     * @return McpSchema.CreateMessageResult
     */
    @Override
    public McpSchema.CreateMessageResult sample(List<McpSchema.SamplingMessage> messages, List<String> models, LLMOptions llmOptions) {
        return this.sampleAsync(messages, models, llmOptions).block();
    }

    /**
     * 向客户端发送异步采样请求 Send asynchronous sampling request to the client
     *
     * @param messages   消息列表 message list
     * @param models     待选模型列表(仅供客户端参考) List of models to be selected (for client reference only)
     * @param llmOptions 模型参数 model parameter
     * @return McpSchema.CreateMessageResult
     */
    @Override
    public Mono<McpSchema.CreateMessageResult> sampleAsync(List<McpSchema.SamplingMessage> messages, List<String> models, LLMOptions llmOptions) {

        McpSchema.CreateMessageRequest sampleRequest = buildSamplingRequest(messages, models, llmOptions);
        return exchange.createMessage(sampleRequest);
    }



}
