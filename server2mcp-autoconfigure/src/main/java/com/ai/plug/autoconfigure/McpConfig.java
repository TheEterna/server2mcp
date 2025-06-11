package com.ai.plug.autoconfigure;

import com.logaritex.mcp.spring.SyncMcpAnnotationProvider;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author 韩
 */
@Configuration
public class McpConfig {
//
//    @Bean
//    public List<McpServerFeatures.SyncCompletionSpecification> syncCompletionSpecifications(
//            List<AutocompleteProvider> completeProviders) {
//        return SpringAiMcpAnnotationProvider.createSyncCompleteSpecifications(completeProviders);
//    }
//
//    @Bean
//    public List<McpServerFeatures.SyncPromptSpecification> syncPromptSpecifications(
//            List<PromptProvider> promptProviders) {
//        return SpringAiMcpAnnotationProvider.createSyncPromptSpecifications(promptProviders);
//    }
    

    
//    @Bean
//    public List<Consumer<McpSchema.LoggingMessageNotification>> syncLoggingConsumers(
//            List<LoggingHandler> loggingHandlers) {
//        return SpringAiMcpAnnotationProvider.createSyncLoggingConsumers(loggingHandlers);
//    }
}