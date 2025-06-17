package com.ai.plug.autoconfigure;

import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.common.utils.ConvertUtil;
import com.ai.plug.core.context.CompleteContext;
import com.ai.plug.core.context.ResourceContext;
import com.ai.plug.core.context.ToolContext;
import com.ai.plug.core.provider.ScannableMethodToolCallbackProvider;
import com.logaritex.mcp.spring.AsyncMcpAnnotationProvider;
import com.logaritex.mcp.spring.SyncMcpAnnotationProvider;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

import static com.ai.plug.common.constants.ConfigConstants.*;

/**
 * @author 她也曾美如画
 * @time: 2025/04
 */
@Configuration
@ComponentScan(basePackages = "com.ai.plug")
@Conditional(Conditions.SystemCondition.class)
@Import({
        McpConfig.AutoConfiguredToolScannerRegistrar.class,
        McpConfig.AutoConfiguredResourceScannerRegistrar.class,
        McpConfig.AutoConfiguredPromptScannerRegistrar.class,
        McpConfig.AutoConfiguredCompleteScannerRegistrar.class,
        ToolConfig.class
})
public class Server2McpAutoConfiguration {
    public static final Logger logger = LoggerFactory.getLogger(Server2McpAutoConfiguration.class);

    /**
     * 创建自定义工具回调, 用于扫描自定义工具, 当false时, 不使用该回调(不启用自动注册功能), 使用spring-ai默认功能
     * @param applicationContext spring容器
     * @param properties 应用参数
     * @return
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_TOOL, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public ToolCallbackProvider customToolCallbackProvider(ApplicationContext applicationContext, PluginProperties properties){
        Map<Object, ToolContext.ToolRegisterDefinition> tools = ToolContext.getRawTools().entrySet().stream().collect(Collectors.toMap(
                entry -> applicationContext.getBean(entry.getKey()),
                Map.Entry::getValue
        ));
        // 需要在这里添加被@ToolScan注解scan到的bean
        return ScannableMethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }

    /**
     * 创建同步资源包括资源模板, 无需担心同步和异步的区别,系统会自动转换
     * @param applicationContext spring容器
     * @return 异步的resources
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_RESOURCE, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.AsyncResourceSpecification> asyncResourceSpecifications(ApplicationContext applicationContext){
        List<Object> resources = ResourceContext.getRawResources().stream().map(applicationContext::getBean).collect(Collectors.toList());
        // 通过sync转换成的async资源, 无需关注异步和同步的区别, 系统会自动转换
        return AsyncMcpAnnotationProvider.createAsyncResourceSpecifications(resources);

    }


    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_PROMPT, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.AsyncPromptSpecification> asyncPromptSpecification(ApplicationContext applicationContext){
        List<Object> prompts = CompleteContext.getRawCompletes().stream().map(applicationContext::getBean).toList();
        return AsyncMcpAnnotationProvider.createAsyncPromptSpecification(prompts);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_COMPLETE, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public List<McpServerFeatures.AsyncCompletionSpecification> asyncCompletionSpecification(ApplicationContext applicationContext){
        List<Object> completions = CompleteContext.getRawCompletes().stream().map(applicationContext::getBean).toList();
        return AsyncMcpAnnotationProvider.createAsyncCompletionSpecification(completions);
    }




}