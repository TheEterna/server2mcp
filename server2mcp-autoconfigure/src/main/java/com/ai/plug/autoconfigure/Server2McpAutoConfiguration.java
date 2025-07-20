package com.ai.plug.autoconfigure;

import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.autoconfigure.spec.AsyncSpecMcpConfig;
import com.ai.plug.autoconfigure.spec.SyncSpecMcpConfig;
import com.ai.plug.core.context.complete.CompleteContextFactory;
import com.ai.plug.core.context.complete.ICompleteContext;
import com.ai.plug.core.context.prompt.IPromptContext;
import com.ai.plug.core.context.prompt.PromptContextFactory;
import com.ai.plug.core.context.resource.IResourceContext;
import com.ai.plug.core.context.resource.ResourceContextFactory;
import com.ai.plug.core.context.root.IRootContext;
import com.ai.plug.core.context.root.RootContextFactory;
import com.ai.plug.core.context.tool.IToolContext;
import com.ai.plug.core.context.tool.ToolContextFactory;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author 她也曾美如画
 * @time: 2025/04
 */
@Configuration
@Conditional(Conditions.SystemCondition.class)
@Import({
        McpConfig.class,
        AsyncSpecMcpConfig.class,
        SyncSpecMcpConfig.class
})
public class Server2McpAutoConfiguration {
    public static final Logger logger = LoggerFactory.getLogger(Server2McpAutoConfiguration.class);










}