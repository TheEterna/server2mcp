package com.ai.plug.core.utils.logging;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * 日志类
 * @author han
 * @time 2025/6/24 16:12
 */

public abstract class McpLogger {


    public abstract Object getServer();
    /**
     * 发送 debug 级别的日志消息
     * @param msg 日志字符串
     */
    public void debug(String msg, boolean isAll) {
        if (!isAll) {
            return;
        }

        Object server = getServer();
        if (server instanceof McpAsyncServer asyncServer) {
            asyncServer.loggingNotification(
                    McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.DEBUG)
                            .logger(msg)
                            .data(msg)
                            .build());
        }else if (server instanceof McpSyncServer syncServer){
            syncServer.loggingNotification(
                    McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.DEBUG)
                            .logger(msg)
                            .data(msg)
                            .build());
        }

    }

    /**
     * 发送 info 级别的日志消息
     * @param msg 日志字符串
     */
    public void info(String msg, boolean isAll){

    }

    /**
     * 发送 notice 级别的日志消息
     * @param msg 日志字符串
     */
    public void notice(String msg, boolean isAll){}

    /**
     * 发送 warning 级别的日志消息
     * @param msg 日志字符串
     */
    public void warning(String msg, boolean isAll){}

    /**
     * 发送 error 级别的日志消息
     * @param msg 日志字符串
     */
    public void error(String msg, boolean isAll){}

    /**
     * 发送 critical 级别的日志消息
     * @param msg 日志字符串
     */
    public void critical(String msg, boolean isAll){}

    /**
     * 发送 alert 级别的日志消息
     * @param msg 日志字符串
     */
    public void alert(String msg, boolean isAll){}

    /**
     * 发送 emergency 级别的日志消息
     * @param msg 日志字符串
     */
    public void emergency(String msg, boolean isAll){
        if (!isAll) {
            emergency(msg);
            return;
        }

        Object server = getServer();
        if (server instanceof McpAsyncServer asyncServer) {
            asyncServer.loggingNotification(
                    McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.EMERGENCY)
                            .logger(msg)
                            .data(msg)
                            .build());
        }else if (server instanceof McpSyncServer syncServer){
            syncServer.loggingNotification(
                    McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.EMERGENCY)
                            .logger(msg)
                            .data(msg)
                            .build());
        }

    }
    
    
    
    /**
     * 发送 debug 级别的日志消息
     * @param msg 日志字符串
     */
    public abstract void debug(String msg);

    /**
     * 发送 info 级别的日志消息
     * @param msg 日志字符串
     */
    public abstract void info(String msg);

    /**
     * 发送 notice 级别的日志消息
     * @param msg 日志字符串
     */
    public abstract void notice(String msg);

    /**
     * 发送 warning 级别的日志消息
     * @param msg 日志字符串
     */
    public abstract void warning(String msg);

    /**
     * 发送 error 级别的日志消息
     * @param msg 日志字符串
     */
    public abstract void error(String msg);

    /**
     * 发送 critical 级别的日志消息
     * @param msg 日志字符串
     */
    public abstract void critical(String msg);

    /**
     * 发送 alert 级别的日志消息
     * @param msg 日志字符串
     */
    public abstract void alert(String msg);

    /**
     * 发送 emergency 级别的日志消息
     * @param msg 日志字符串
     */
    public abstract void emergency(String msg);


}
