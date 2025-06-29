package com.ai.plug.autoconfigure;

import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.autoconfigure.spec.AsyncSpecMcpConfig;
import com.ai.plug.autoconfigure.spec.SyncSpecMcpConfig;
import com.ai.plug.core.context.CompleteContext;
import com.ai.plug.core.context.ResourceContext;
import com.ai.plug.core.context.ToolContext;
import com.ai.plug.core.provider.ScannableMethodToolCallbackProvider;
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


}