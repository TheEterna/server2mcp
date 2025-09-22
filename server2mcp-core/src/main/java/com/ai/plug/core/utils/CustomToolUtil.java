package com.ai.plug.core.utils;

import com.ai.plug.core.context.root.IRootContext;
import com.ai.plug.core.provider.CustomToolCallResultConverter;
import com.ai.plug.core.register.tool.McpToolScanRegistrar;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 韩
 * time: 2025/5/14 0:28
 */

public class CustomToolUtil {


    public static List<ToolCallback> getToolCallbackListByName(List<ToolCallback> toolCallbackList, String name) {
        return toolCallbackList.stream().filter((toolCallback) -> {
            return toolCallback.getToolDefinition().name().equals(name);
        }).collect(Collectors.toList());



    }

    public static Object mcpInjection(Object exchange, IRootContext rootContext) throws Exception{

        // 避免重复注入 roots
        // Avoid duplicate injection of roots
        List<McpSchema.Root> rootList = rootContext.getRoots(exchange);
        if (rootList != null && !rootList.isEmpty()) {
            return null;
        }

        if (exchange instanceof McpSyncServerExchange syncExchange) {

            McpSchema.ListRootsResult listRootsResult = syncExchange.listRoots();
            List<McpSchema.Root> roots = listRootsResult.roots();
            rootContext.setRoots(exchange, roots);

        } else if (exchange instanceof McpAsyncServerExchange asyncExchange) {

            McpSchema.ListRootsResult listRootsResult = asyncExchange.listRoots().block();
            List<McpSchema.Root> roots = listRootsResult.roots();
            rootContext.setRoots(exchange, roots);
        } else {
            throw new IllegalArgumentException("exchange must be McpSyncServerExchange or McpAsyncServerExchange");
        }
        return null;
    }


    public static ToolCallResultConverter getToolCallResultConverter(Method method) {
        Assert.notNull(method, "method cannot be null");
        Tool tool = method.getAnnotation(Tool.class);
        if (tool == null) {
            return new CustomToolCallResultConverter();
        } else {
            Class<? extends ToolCallResultConverter> type = tool.resultConverter();
            if (ClassUtils.isAssignable(type.getDeclaringClass(), DefaultToolCallResultConverter.class)) {
                return new CustomToolCallResultConverter();
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new IllegalArgumentException("Failed to instantiate ToolCallResultConverter: " + String.valueOf(type), ex);
            }
        }
    }


    /**
     * 生成的bean名字函数, 生成的名字是: 类名 + McpToolScanRegistrar.class的名字 + index ,中间 # 分t
     * @param importingClassMetadata
     * @param index
     * @return
     */
    public static String generateBaseBeanName(AnnotationMetadata importingClassMetadata, int index) {
        String className = importingClassMetadata.getClassName();
        return className + "#" + McpToolScanRegistrar.class.getSimpleName() + "#" + index;
    }

    /**
     * 基于导入类的元数据信息，推导并返回默认的基础包名。
     *
     * <p>此方法会提取导入类所在的包路径，将其作为组件扫描的基础包。
     * 例如，若导入类为"com.example.config.AppConfig"，则返回的基础包为"com.example.config"。
     * 这在自动配置场景中尤为有用，可避免手动指定扫描路径，实现约定大于配置的原则。
     *
     * @param importingClassMetadata 导入类的注解元数据，不能为null
     * @return 返回从导入类中提取的包名，不会返回null
     * @throws IllegalArgumentException 当导入类元数据为null时抛出此异常
     *
     * @see ClassUtils#getPackageName(String) 用于获取类的包名的工具方法
     * @param importingClassMetadata
     * @return
     */
    public static String getDefaultBasePackage(AnnotationMetadata importingClassMetadata) {
        return ClassUtils.getPackageName(importingClassMetadata.getClassName());
    }


    /**
     * 通过反射获取McpServerSession对象，支持同步和异步Exchange
     * @param exchange 可以是McpSyncServerExchange或McpAsyncServerExchange实例
     * @return McpServerSession对象
     * @throws NoSuchFieldException 如果找不到相应字段
     * @throws IllegalAccessException 如果无法访问该字段
     * @throws IllegalArgumentException 如果传入的对象类型不支持
     */
    public static McpServerSession getSession(Object exchange)
            throws NoSuchFieldException, IllegalAccessException {

        if (exchange == null) {
            throw new IllegalArgumentException("exchange cannot be null");
        }

        Object targetExchange = exchange;

        // 处理同步Exchange嵌套结构
        if (exchange instanceof McpSyncServerExchange syncExchange) {
            targetExchange = getInternalExchange(syncExchange);
        }

        // 统一处理异步Exchange及其内部对象
        if (targetExchange instanceof McpAsyncServerExchange asyncExchange) {
            return getFieldValue(asyncExchange, "session");
        }

        throw new IllegalArgumentException("Unsupported exchange type: " + exchange.getClass().getName() + ", exchange must be McpSyncServerExchange or McpAsyncServerExchange");
    }

    /**
     * 从McpSyncServerExchange中获取内部的McpAsyncServerExchange
     */
    private static McpAsyncServerExchange getInternalExchange(McpSyncServerExchange syncExchange)
            throws NoSuchFieldException, IllegalAccessException {
        return getFieldValue(syncExchange, "exchange");
    }

    /**
     * 通用反射工具方法：获取对象的私有字段值
     */
    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        if (obj == null) {
            return null;
        }

        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }

}