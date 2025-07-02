package com.ai.plug.core.spec.utils.logging;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/**
 * @author han
 * @time 2025/6/24 3:05
 */

public class McpAsyncLogger extends McpLogger{

    private final static Logger logger = LoggerFactory.getLogger(McpAsyncLogger.class);

    private final McpAsyncServerExchange exchange;

    private final McpAsyncServer asyncServer;

    private final Class<?> clazz;


    public McpAsyncLogger(McpAsyncServer asyncServer, McpAsyncServerExchange exchange, @Nullable Class<?> clazz) {
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
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }

    public void info(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.INFO)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }
    public void notice(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.NOTICE)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }
    public void warning(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.WARNING)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }
    public void error(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.ERROR)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }
    public void critical(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.CRITICAL)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }
    public void alert(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.ALERT)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }
    public void emergency(String msg) {
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.EMERGENCY)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }



}
