package com.ai.plug.common.utils;

import java.util.Map;

/**
 * @ author 韩
 * time: 2025/4/29 0:05
 */

public class ContextUtil {

    private static final ThreadLocal<Map<String, Object>> context = new ThreadLocal<>();

    public static void setAttribute(String key, Object value) {
        // 设置当前线程的上下文信息

        Map<String, Object> map = context.get();
        if (map != null) {
            map.put(key, value);
        } else {
            context.set(Map.of(key, value));
        }
    }

    public static Object getContext(String key) {
        // 获取当前线程的上下文信息
        if (context.get() != null){
            return context.get().get(key);
        }


        return context.get();
    }
    public static Object getContexts() {


        return context.get();
    }

    public static void clearContext() {
        // 清除当前线程的上下文信息
        context.remove();
    }

    public static void main(String[] args) {
    }
}
