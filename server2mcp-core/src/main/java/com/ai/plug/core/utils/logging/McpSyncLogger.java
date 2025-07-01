package com.ai.plug.core.utils.logging;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author han
 * @time 2025/6/24 16:16
 */
public class McpSyncLogger extends McpLogger {


    private final Logger logger = LoggerFactory.getLogger(McpSyncLogger.class);
    private final McpSyncServerExchange exchange;

    private final McpSyncServer syncServer;

    private final Class<?> clazz;

    public McpSyncLogger(McpSyncServer syncServer, McpSyncServerExchange exchange, Class<?> clazz) {
        this.syncServer = syncServer;
        this.exchange = exchange;
        this.clazz = clazz;
    }

    @Override
    public Object getServer() {
        return this.syncServer;
    }

    /**
     * 发送 debug 级别的日志消息
     *
     * @param msg 日志字符串
     */
    @Override
    public void debug(String msg) {
        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return;
        }
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.DEBUG)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }

    /**
     * 发送 info 级别的日志消息
     *
     * @param msg 日志字符串
     */
    @Override
    public void info(String msg) {
        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return;
        }
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.INFO)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }




    /**
     * 发送 notice 级别的日志消息
     *
     * @param msg 日志字符串
     */
    @Override
    public void notice(String msg) {
        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return;
        }
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.NOTICE)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }

    /**
     * 发送 warning 级别的日志消息
     *
     * @param msg 日志字符串
     */
    @Override
    public void warning(String msg) {
        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return;
        }
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.WARNING)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }
    /**
     * 发送 error 级别的日志消息
     *
     * @param msg 日志字符串
     */
    @Override
    public void error(String msg) {
        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return;
        }
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.ERROR)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }

    /**
     * 发送 critical 级别的日志消息
     *
     * @param msg 日志字符串
     */
    @Override
    public void critical(String msg) {
        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return;
        }
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.CRITICAL)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }

    /**
     * 发送 alert 级别的日志消息
     *
     * @param msg 日志字符串
     */
    @Override
    public void alert(String msg) {
        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return;
        }
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.ALERT)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }


    /**
     * 发送 emergency 级别的日志消息
     *
     * @param msg 日志字符串
     */
    @Override
    public void emergency(String msg) {
        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return;
        }
        exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.EMERGENCY)
                        .logger(clazz.getName())
                        .data(msg)
                        .build());
    }
}
