package com.ai.plug.autoconfigure;

import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.autoconfigure.spec.AsyncSpecMcpConfig;
import com.ai.plug.autoconfigure.spec.SyncSpecMcpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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