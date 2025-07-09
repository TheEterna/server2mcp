package com.ai.plug.core.spec.utils.logging;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

/**
 * @author han
 * @time 2025/6/24 3:05
 */

public class McpAsyncLogger implements McpLogger{

    private final static Logger logger = LoggerFactory.getLogger(McpAsyncLogger.class);

    private final McpAsyncServerExchange exchange;

    private final McpAsyncServer asyncServer;

    private final Class<?> clazz;


    public McpAsyncLogger(McpAsyncServer asyncServer, McpAsyncServerExchange exchange, @Nullable Class<?> clazz) {
        this.asyncServer = asyncServer;
        this.exchange = exchange;
        this.clazz = clazz;
    }

//    @Override
//    public Object getServer() {
//        return this.asyncServer;
//    }

    @Override
    public void debug(String msg) {

        this.debugAsync(msg).block();
    }

    @Override
    public void info(String msg) {

        this.infoAsync(msg).block();
    }

    @Override
    public void notice(String msg) {

        this.noticeAsync(msg).block();

    }
    @Override
    public void warning(String msg) {

        this.warningAsync(msg).block();
    }
    @Override
    public void error(String msg) {

        this.errorAsync(msg).block();

    }
    @Override
    public void critical(String msg) {

        this.criticalAsync(msg).block();

    }
    @Override
    public void alert(String msg) {

        this.alertAsync(msg).block();

    }
    @Override
    public void emergency(String msg) {

        this.emergencyAsync(msg).block();

    }


    @Override
    public Mono<Void> debugAsync(String msg) {
        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return Mono.empty();
        }

        return exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.DEBUG)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }

    @Override
    public Mono<Void> infoAsync(String msg) {

        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return Mono.empty();
        }
        return exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.INFO)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }

    @Override
    public Mono<Void> noticeAsync(String msg) {

        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return Mono.empty();
        }
        return exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.NOTICE)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }

    @Override
    public Mono<Void> warningAsync(String msg) {

        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return Mono.empty();
        }
        return exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.WARNING)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }

    @Override
    public Mono<Void> errorAsync(String msg) {

        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return Mono.empty();
        }
        return exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.ERROR)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }

    @Override
    public Mono<Void> criticalAsync(String msg) {

        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return Mono.empty();
        }
        return exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.CRITICAL)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }

    @Override
    public Mono<Void> alertAsync(String msg) {

        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return Mono.empty();
        }
        return exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.ALERT)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }

    @Override
    public Mono<Void> emergencyAsync(String msg) {

        if (exchange == null) {
            logger.warn("exchange is null, cannot find the corresponding client to send logs");
            return Mono.empty();
        }
        return exchange.loggingNotification(
                McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.EMERGENCY)
                        .logger(asyncServer.toString() + (clazz == null ? null : clazz.getName()))
                        .data(msg)
                        .build());
    }


}
