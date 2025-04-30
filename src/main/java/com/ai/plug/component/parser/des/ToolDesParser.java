package com.ai.plug.component.parser.des;

import com.ai.plug.component.config.PluginProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;


/**
 * @ author 韩
 * time: 2025/4/29 23:18
 */
@Component("ToolDesParser")
public class ToolDesParser extends AbstractDesParser{
    @Override
    public String doParse(Method method, Class<?> toolClass) {
        Tool toolAnnotation = method.getAnnotation(Tool.class);

        String des = toolAnnotation.description();

        if (StringUtils.hasText(des)) {
            return des;
        }
        return null;
    }

    @Override
    protected PluginProperties.ParserType getName() {
        return null;
    }
}
