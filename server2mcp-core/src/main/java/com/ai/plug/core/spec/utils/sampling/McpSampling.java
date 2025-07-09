package com.ai.plug.core.spec.utils.sampling;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author han
 * @time 2025/7/8 18:08
 */

public sealed interface McpSampling permits McpAsyncSampling, McpSyncSampling {

//    List<McpSchema.SamplingMessage> messages,
//    McpSchema.ModelPreferences modelPreferences,
//    String systemPrompt,
//    McpSchema.CreateMessageRequest.ContextInclusionStrategy includeContext,
    default McpSchema.CreateMessageRequest buildSamplingRequest(List<McpSchema.SamplingMessage> messages, List<String> models, LLMOptions llmOptions) {
        // build args for send sampling request
        List<McpSchema.ModelHint> modelHints = models.stream().map(McpSchema.ModelHint::of)
                .toList();
        McpSchema.ModelPreferences modelPreferences = McpSchema.ModelPreferences.builder()
                .hints(modelHints)
                .build();

        McpSchema.CreateMessageRequest sampleRequest = McpSchema.CreateMessageRequest.builder()
                .messages(messages)
                .modelPreferences(modelPreferences)
                .systemPrompt(llmOptions.systemPrompt())
                .includeContext(llmOptions.includeContext())
                .temperature(llmOptions.temperature())
                .maxTokens(llmOptions.maxTokens())
                .stopSequences(llmOptions.stopSequences()).build();
        return sampleRequest;
    }
    record LLMOptions(
            // 模型温度
            Double temperature,
            // 最大token
            int maxTokens,
            // 系统提示词
            String systemPrompt,
            // 停止序列
            List<String> stopSequences,
            // 上下文策略
            McpSchema.CreateMessageRequest.ContextInclusionStrategy includeContext
    ) {


        public static class Builder {
            private Double temperature;
            private int maxTokens;
            // 系统提示词
            private String systemPrompt;
            private List<String> stopSequences;
            private McpSchema.CreateMessageRequest.ContextInclusionStrategy includeContext;

            public Builder temperature(Double temperature) {
                this.temperature = temperature;
                return this;
            }
            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }
            public Builder systemPrompt(String systemPrompt) {
                this.systemPrompt = systemPrompt;
                return this;
            }
            public Builder stopSequences(List<String> stopSequences) {
                this.stopSequences = stopSequences;
                return this;
            }
            public Builder includeContext(McpSchema.CreateMessageRequest.ContextInclusionStrategy includeContext) {
                this.includeContext = includeContext;
                return this;
            }
            public LLMOptions build() {
                return new LLMOptions(temperature, maxTokens, systemPrompt, stopSequences, includeContext);
            }
        }
    }

    /**
     * 向客户端发送同步采样请求 Send synchronous sampling request to the client
     * @param messages 消息列表 message list
     * @param models 待选模型列表(仅供客户端参考) List of models to be selected (for client reference only)
     * @param llmOptions 模型参数 model parameter
     * @return McpSchema.CreateMessageResult
     */
    McpSchema.CreateMessageResult sample(List<McpSchema.SamplingMessage> messages,
                                         @Nullable List<String> models, @Nullable LLMOptions llmOptions);


    default McpSchema.CreateMessageResult sample(List<McpSchema.SamplingMessage> messages,
                                         @Nullable List<String> models) {
        return this.sample(messages, models, null);
    }


    default McpSchema.CreateMessageResult sample(List<McpSchema.SamplingMessage> messages,
                                         @Nullable String model) {
        return this.sample(messages, List.of(model), null);
    }


    default McpSchema.CreateMessageResult sample(List<McpSchema.SamplingMessage> messages,
                                          @Nullable LLMOptions llmOptions) {
        return this.sample(messages, null, llmOptions);
    }

    default McpSchema.CreateMessageResult sample(List<McpSchema.SamplingMessage> messages) {
        return this.sample(messages, null, null);
    }

    default McpSchema.CreateMessageResult sample(String message) {

        McpSchema.SamplingMessage samplingMessage = new McpSchema.SamplingMessage(McpSchema.Role.USER, new McpSchema.TextContent(message));

        return this.sample(List.of(samplingMessage), null, null);
    }



    /**
     * 向客户端发送异步采样请求 Send asynchronous sampling request to the client
     * @param messages 消息列表 message list
     * @param models 待选模型列表(仅供客户端参考) List of models to be selected (for client reference only)
     * @param llmOptions 模型参数 model parameter
     * @return McpSchema.CreateMessageResult
     */
    Mono<McpSchema.CreateMessageResult> sampleAsync(List<McpSchema.SamplingMessage> messages,
                                        @Nullable List<String> models, @Nullable LLMOptions llmOptions);

    default Mono<McpSchema.CreateMessageResult> sampleAsync(List<McpSchema.SamplingMessage> messages,
                                                 @Nullable List<String> models) {
        return this.sampleAsync(messages, models, null);
    }


    default Mono<McpSchema.CreateMessageResult> sampleAsync(List<McpSchema.SamplingMessage> messages,
                                                 @Nullable String model) {
        return this.sampleAsync(messages, List.of(model), null);
    }


    default Mono<McpSchema.CreateMessageResult> sampleAsync(List<McpSchema.SamplingMessage> messages,
                                                 @Nullable LLMOptions llmOptions) {
        return this.sampleAsync(messages, null, llmOptions);
    }

    default Mono<McpSchema.CreateMessageResult> sampleAsync(List<McpSchema.SamplingMessage> messages) {
        return this.sampleAsync(messages, null, null);
    }

    default Mono<McpSchema.CreateMessageResult> sampleAsync(String message) {

        McpSchema.SamplingMessage samplingMessage = new McpSchema.SamplingMessage(McpSchema.Role.USER, new McpSchema.TextContent(message));

        return this.sampleAsync(List.of(samplingMessage), null, null);
    }



}
