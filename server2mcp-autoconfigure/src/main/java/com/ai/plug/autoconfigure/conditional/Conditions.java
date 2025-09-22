package com.ai.plug.autoconfigure.conditional;

import com.ai.plug.autoconfigure.PluginProperties;
import com.ai.plug.core.parser.tool.des.AbstractDesParser;
import com.ai.plug.core.parser.tool.param.AbstractParamParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

import static com.ai.plug.common.constants.ConfigConstants.*;

/**
 * @author: han
 * time: 2025/04/2025/4/12 23:38
 * des:
 */
@Slf4j
public class Conditions {
    public static class SystemCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();


            return environment.getProperty(VARIABLE_PREFIX + "." + "enabled", Boolean.class, false);
        }
    }

    /**
     * 插件作用域为接口时才生效
     */
    public static class IsInterfaceCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            PluginProperties.ScopeType type = environment.getProperty(VARIABLE_PREFIX + "." + "scope", PluginProperties.ScopeType.class, PluginProperties.ScopeType.INTERFACE);
            return type.equals(PluginProperties.ScopeType.INTERFACE);
        }
    }
    public static class IsSyncCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            McpServerProperties.ServerType type = environment.getProperty(SPRING_AI_VARIABLE_PREFIX + "." + VARIABLE_TYPE, McpServerProperties.ServerType.class, McpServerProperties.ServerType.SYNC);
            return type.equals(McpServerProperties.ServerType.SYNC);
        }
    }
    public static class IsAsyncCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            McpServerProperties.ServerType type = environment.getProperty(SPRING_AI_VARIABLE_PREFIX + "." + VARIABLE_TYPE, McpServerProperties.ServerType.class, McpServerProperties.ServerType.SYNC);
            return type.equals(McpServerProperties.ServerType.ASYNC);
        }
    }


    public static class ParserCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            // 获取配置文件中的 parsers.types 属性
            Environment env = context.getEnvironment();
            Class<?> parserType = (Class<?>) metadata.getAnnotationAttributes(ConditionalOnParser.class.getName()).get("type");
            // 从注解元数据中获取要匹配的动物类型
            String parser = (String) metadata.getAnnotationAttributes(ConditionalOnParser.class.getName()).get("value");
            String[] types;


            if (ClassUtils.isAssignable(parserType, AbstractParamParser.class)) {
                types = env.getProperty(VARIABLE_PREFIX +'.' + PARSER_PREFIX + '.' + VARIABLE_PARAM, String[].class, new String[0]);
                types = types.length == 0 ? new String[]{"MCPTOOL", "TOOL", "JACKSON", "SPRINGMVC", "JAVADOC", "SWAGGER2", "SWAGGER3"} : types;

            }
            else if (ClassUtils.isAssignable(parserType, AbstractDesParser.class)) {
                types = env.getProperty(VARIABLE_PREFIX +'.' + PARSER_PREFIX + '.' + VARIABLE_DES, String[].class, new String[0]);
                types = types.length == 0 ? new String[]{"MCPTOOL", "TOOL", "JACKSON", "JAVADOC", "SWAGGER2", "SWAGGER3"} : types;
            } else {
                log.warn("not have the parserType");
                return false;
            }

            // 判断配置中是否包含该动物类型
            for (String type : types) {
                if (type.equalsIgnoreCase(parser)) {
                    return true;
                }
            }
            return false;
        }
    }


    public static class RootCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return environment.getProperty(VARIABLE_PREFIX + VARIABLE_ROOT + VARIABLE_ENABLED, Boolean.class, true);
        }
    }
    public static class ToolCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return environment.getProperty(VARIABLE_PREFIX + VARIABLE_TOOL + VARIABLE_ENABLED, Boolean.class, true);
        }
    }
    public static class ResourceCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return environment.getProperty(VARIABLE_PREFIX + VARIABLE_RESOURCE + VARIABLE_ENABLED, Boolean.class, true);
        }
    }
    public static class PromptCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return environment.getProperty(VARIABLE_PREFIX + VARIABLE_PROMPT + VARIABLE_ENABLED, Boolean.class, true);
        }
    }
    public static class CompleteCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return environment.getProperty(VARIABLE_PREFIX + VARIABLE_COMPLETE + VARIABLE_ENABLED, Boolean.class, true);
        }
    }




}
