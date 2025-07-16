package com.ai.plug.autoconfigure.spec;

import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.core.builder.ToolDefinitionBuilder;
import com.ai.plug.core.context.complete.ICompleteContext;
import com.ai.plug.core.context.prompt.IPromptContext;
import com.ai.plug.core.context.resource.IResourceContext;
import com.ai.plug.core.context.root.IRootContext;
import com.ai.plug.core.context.tool.IToolContext;
import com.ai.plug.core.context.tool.ToolContext;
import com.ai.plug.core.springai.provider.AsyncMcpAnnotationProvider;
import io.modelcontextprotocol.server.McpServerFeatures;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ai.plug.common.constants.ConfigConstants.*;

/**
 * Async MCP function configuration class
 * 异步 MCP 功能配置类
 * @author han
 * @time 2025/6/20 16:51
 */
@Configuration
@Conditional(Conditions.IsAsyncCondition.class)
public class AsyncSpecMcpConfig {




    /**
     * 创建异步 Tool
     * Creates asynchronous Tool specifications
     * @param applicationContext spring容器
     * @return 异步的tools
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_TOOL, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.AsyncToolSpecification> asyncToolSpecifications(
            ApplicationContext applicationContext,
            ToolDefinitionBuilder builder,
            IRootContext rootContext,
            IToolContext toolContext) {

        Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions = toolContext.getRawTools().entrySet().stream().collect(Collectors.toMap(
                entry -> applicationContext.getBean(entry.getKey()),
                Map.Entry::getValue
        ));
        return AsyncMcpAnnotationProvider.createAsyncToolSpecifications(toolAndDefinitions, builder, rootContext);
    }


    /**
     * 创建异步 资源(包括资源模板)
     * Creates asynchronous resources (including resource templates)
     * @param applicationContext spring容器
     * @return 异步的resources
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_RESOURCE, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.AsyncResourceSpecification> asyncResourceSpecifications(
            ApplicationContext applicationContext,
            IResourceContext resourceContext
    ){
        List<Object> resources = resourceContext.getRawResources().stream().map(applicationContext::getBean).collect(Collectors.toList());
        // 通过sync转换成的async资源, 无需关注异步和同步的区别, 系统会自动转换
        return AsyncMcpAnnotationProvider.createAsyncResourceSpecifications(resources);

    }


    /**
     * 创建异步 Prompt
     * Creates asynchronous Prompt specifications
     * @param applicationContext
     * @return
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_PROMPT, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.AsyncPromptSpecification> asyncPromptSpecification(
            ApplicationContext applicationContext,
            IPromptContext promptContext){
        List<Object> prompts = promptContext.getRawPrompts().stream().map(applicationContext::getBean).toList();
        return AsyncMcpAnnotationProvider.createAsyncPromptSpecifications(prompts);
    }

    /**
     * 创建异步 Completion
     * Creates asynchronous Completion specifications
     * @param applicationContext
     * @return
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_COMPLETE, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.AsyncCompletionSpecification> asyncCompletionSpecification(
            ApplicationContext applicationContext,
            ICompleteContext completeContext){
        List<Object> completions = completeContext.getRawCompletes().stream().map(applicationContext::getBean).toList();
        return AsyncMcpAnnotationProvider.createAsyncCompletionSpecifications(completions);
    }






}
