package com.ai.plug.core.utils.logging;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author han
 * @time 2025/6/24 3:05
 */

public class McpAsyncLogger extends McpLogger{

    private final static Logger logger = LoggerFactory.getLogger(McpAsyncLogger.class);

    private final McpAsyncServerExchange exchange;

    private final McpAsyncServer asyncServer;

    private final Class<?> clazz;


    public McpAsyncLogger(McpAsyncServer asyncServer, McpAsyncServerExchange exchange, Class<?> clazz) {
        this.asyncServer = asyncServer;
        this.exchange = exchange;
        this.clazz = clazz;
    }

    @Override
    public Object getServer() {
        return this.asyncServer;
    }

    public void debug(String msg) {
        exchange.loggingNotification(
                 McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.DEBUG)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }

    public void info(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.INFO)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }
    public void notice(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.NOTICE)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }
    public void warning(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.WARNING)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }
    public void error(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.ERROR)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }
    public void critical(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.CRITICAL)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }
    public void alert(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.ALERT)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }
    public void emergency(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.EMERGENCY)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }



}
