package com.ai.plug.core.utils;

import com.ai.plug.core.provider.CustomToolCallResultConverter;
import com.ai.plug.core.register.tool.ToolScanRegistrar;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

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
            } catch (Exception var4) {
                throw new IllegalArgumentException("Failed to instantiate ToolCallResultConverter: " + String.valueOf(type), var4);
            }
        }
    }


    /**
     * 生成的bean名字函数, 生成的名字是: 类名 + ToolScanRegistrar.class的名字 + index ,中间 # 分t
     * @param importingClassMetadata
     * @param index
     * @return
     */
    public static String generateBaseBeanName(AnnotationMetadata importingClassMetadata, int index) {
        String className = importingClassMetadata.getClassName();
        return className + "#" + ToolScanRegistrar.class.getSimpleName() + "#" + index;
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

}