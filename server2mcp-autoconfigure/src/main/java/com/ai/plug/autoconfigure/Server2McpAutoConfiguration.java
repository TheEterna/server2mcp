package com.ai.plug.autoconfigure;

import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.autoconfigure.spec.AsyncSpecMcpConfig;
import com.ai.plug.autoconfigure.spec.SyncSpecMcpConfig;
import com.ai.plug.core.context.CompleteContext;
import com.ai.plug.core.context.ResourceContext;
import com.ai.plug.core.context.ToolContext;
import com.ai.plug.core.provider.ScannableMethodToolCallbackProvider;
import com.logaritex.mcp.spring.AsyncMcpAnnotationProvider;
import io.modelcontextprotocol.server.McpServerFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

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
        ToolScanConfig.class,
        AsyncSpecMcpConfig.class,
        SyncSpecMcpConfig.class

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






}