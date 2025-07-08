package com.ai.plug.core.spec.utils.logging;

/**
 * 通过 exchange 向客户端发送日志消息
 * Sends log messages to the client via exchange
 * @author han
 * @time 2025/6/24 16:12
 */

public interface McpLogger {


    Object getServer();


    // 把这条进行注释其实是有考虑的, 官方已经不再推荐使用server直接通知所有客户端, 而且在回调函数中也不能拿到server, 只能
//    /**
//     * 发送 debug 级别的日志消息
//     * @param msg 日志字符串
//     */
//    public void debug(String msg, boolean isAll) {
//        if (!isAll) {
//            return;
//        }
//
//        Object server = getServer();
//        if (server instanceof McpAsyncServer asyncServer) {
//            asyncServer.loggingNotification(
//                    McpSchema.LoggingMessageNotification.builder()
//                            .level(McpSchema.LoggingLevel.DEBUG)
//                            .logger(msg)
//                            .data(msg)
//                            .build());
//        }else if (server instanceof McpSyncServer syncServer){
//            syncServer.loggingNotification(
//                    McpSchema.LoggingMessageNotification.builder()
//                            .level(McpSchema.LoggingLevel.DEBUG)
//                            .logger(msg)
//                            .data(msg)
//                            .build());
//        }
//
//    }
//
//    /**
//     * 发送 info 级别的日志消息
//     * @param msg 日志字符串
//     */
//    public void info(String msg, boolean isAll){
//
//    }
//
//    /**
//     * 发送 notice 级别的日志消息
//     * @param msg 日志字符串
//     */
//    public void notice(String msg, boolean isAll){}
//
//    /**
//     * 发送 warning 级别的日志消息
//     * @param msg 日志字符串
//     */
//    public void warning(String msg, boolean isAll){}
//
//    /**
//     * 发送 error 级别的日志消息
//     * @param msg 日志字符串
//     */
//    public void error(String msg, boolean isAll){}
//
//    /**
//     * 发送 critical 级别的日志消息
//     * @param msg 日志字符串
//     */
//    public void critical(String msg, boolean isAll){}
//
//    /**
//     * 发送 alert 级别的日志消息
//     * @param msg 日志字符串
//     */
//    public void alert(String msg, boolean isAll){}
//
//    /**
//     * 发送 emergency 级别的日志消息
//     * @param msg 日志字符串
//     */
//    public void emergency(String msg, boolean isAll){
//        if (!isAll) {
//            emergency(msg);
//            return;
//        }
//
//        Object server = getServer();
//        if (server instanceof McpAsyncServer asyncServer) {
//            asyncServer.loggingNotification(
//                    McpSchema.LoggingMessageNotification.builder()
//                            .level(McpSchema.LoggingLevel.EMERGENCY)
//                            .logger(msg)
//                            .data(msg)
//                            .build());
//        }else if (server instanceof McpSyncServer syncServer){
//            syncServer.loggingNotification(
//                    McpSchema.LoggingMessageNotification.builder()
//                            .level(McpSchema.LoggingLevel.EMERGENCY)
//                            .logger(msg)
//                            .data(msg)
//                            .build());
//        }
//
//    }
//
    /**
     * 发送 debug 级别的日志消息
     * @param msg 日志字符串
     */
    void debug(String msg);

    /**
     * 发送 info 级别的日志消息
     * @param msg 日志字符串
     */
    void info(String msg);

    /**
     * 发送 notice 级别的日志消息
     * @param msg 日志字符串
     */
    void notice(String msg);

    /**
     * 发送 warning 级别的日志消息
     * @param msg 日志字符串
     */
    void warning(String msg);

    /**
     * 发送 error 级别的日志消息
     * @param msg 日志字符串
     */
    void error(String msg);

    /**
     * 发送 critical 级别的日志消息
     * @param msg 日志字符串
     */
    void critical(String msg);

    /**
     * 发送 alert 级别的日志消息
     * @param msg 日志字符串
     */
    void alert(String msg);

    /**
     * 发送 emergency 级别的日志消息
     * @param msg 日志字符串
     */
    void emergency(String msg);


}
