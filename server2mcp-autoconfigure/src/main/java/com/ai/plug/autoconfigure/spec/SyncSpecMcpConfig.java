package com.ai.plug.autoconfigure.spec;

import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.core.builder.ToolDefinitionBuilder;
import com.ai.plug.core.context.complete.CompleteContext;
import com.ai.plug.core.context.complete.ICompleteContext;
import com.ai.plug.core.context.prompt.IPromptContext;
import com.ai.plug.core.context.resource.IResourceContext;
import com.ai.plug.core.context.resource.ResourceContext;
import com.ai.plug.core.context.tool.IToolContext;
import com.ai.plug.core.context.tool.ToolContext;
import com.ai.plug.core.context.root.IRootContext;
import com.ai.plug.core.springai.provider.SyncMcpAnnotationProvider;
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
 * Sync MCP function configuration class
 * 同步 MCP 功能配置类
 * @author han
 * @time 2025/6/20 16:51
 */
@Configuration
@Conditional(Conditions.IsSyncCondition.class)
public class SyncSpecMcpConfig {


    /**
     * 创建同步 工具
     * @param applicationContext spring容器
     * @return Sync tools
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_TOOL, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.SyncToolSpecification> asyncToolSpecifications(
            ApplicationContext applicationContext,
            ToolDefinitionBuilder builder,
            IRootContext rootContext,
            IToolContext toolContext) {

        Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions = toolContext.getRawTools().entrySet().stream().collect(Collectors.toMap(
                entry -> applicationContext.getBean(entry.getKey()),
                Map.Entry::getValue
        ));
        return SyncMcpAnnotationProvider.createSyncToolSpecifications(toolAndDefinitions, builder, rootContext);
    }


    /**
     * 创建同步 资源(包括资源模板)
     * Creates synchronous resources (including resource templates)
     * @param applicationContext spring容器
     * @return Sync resources
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_RESOURCE, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.SyncResourceSpecification> syncResourceSpecifications(
            ApplicationContext applicationContext,
            IResourceContext resourceContext){
        List<Object> resources = resourceContext.getRawResources().stream().map(applicationContext::getBean).collect(Collectors.toList());
        // 通过sync转换成的sync资源, 无需关注同步和同步的区别, 系统会自动转换
        return SyncMcpAnnotationProvider.createSyncResourceSpecifications(resources);

    }


    /**
     * 创建同步 Prompt
     * Creates synchronous Prompt specifications
     * @param applicationContext spring容器
     * @return Sync prompts
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_PROMPT, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.SyncPromptSpecification> syncPromptSpecification(
            ApplicationContext applicationContext,
            IPromptContext promptContext){
        List<Object> prompts = promptContext.getRawPrompts().stream().map(applicationContext::getBean).toList();
        return SyncMcpAnnotationProvider.createSyncPromptSpecifications(prompts);
    }

    /**
     * 创建同步 Completion
     * Creates synchronous Completion specifications
     * @param applicationContext spring容器
     * @return Sync completions
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_COMPLETE, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.SyncCompletionSpecification> syncCompletionSpecification(
            ApplicationContext applicationContext,
            ICompleteContext completeContext){
        List<Object> completions = completeContext.getRawCompletes().stream().map(applicationContext::getBean).toList();
        return SyncMcpAnnotationProvider.createSyncCompleteSpecifications(completions);
    }
}
