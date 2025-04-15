package com.ai.plug.component.conditional;

import com.ai.plug.component.config.PluginProperties;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static com.ai.plug.common.constants.ConfigConstants.VARIABLE_PREFIX;

/**
 * @author: han
 * time: 2025/04/2025/4/12 23:38
 * des:
 */
public class Conditions {
    public static class SystemCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();


            Boolean flag = environment.getProperty(VARIABLE_PREFIX + "." + "enabled", Boolean.class, false);
            return flag;
        }
    }
    public static class IsInterfaceCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            PluginProperties.ScopeType type = environment.getProperty(VARIABLE_PREFIX + "." + "scope", PluginProperties.ScopeType.class, PluginProperties.ScopeType.INTERFACE);
            return type.equals(PluginProperties.ScopeType.INTERFACE);
        }
    }


}
