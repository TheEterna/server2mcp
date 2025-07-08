package com.ai.plug.core.parser.tool.des;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;


/**
 * @author 韩
 * time: 2025/4/29 23:18
 */
public class ToolDesParser extends AbstractDesParser{
    @Override
    public String doDesParse(Method method, Class<?> toolClass) {
        Tool toolAnnotation = method.getAnnotation(Tool.class);

        if (toolAnnotation == null) {
            return null;
        }

        String des = toolAnnotation.description();
        if (StringUtils.hasText(des)) {
            return des;
        }
        return null;
    }
}
