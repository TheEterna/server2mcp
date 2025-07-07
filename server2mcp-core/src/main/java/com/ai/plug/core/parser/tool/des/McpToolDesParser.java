package com.ai.plug.core.parser.tool.des;

import com.logaritex.mcp.annotation.McpTool;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * @author han
 * @time 2025/6/30 3:44
 */

public class McpToolDesParser extends AbstractDesParser{
    @Override
    public String doDesParse(Method method, Class<?> toolClass) {
        McpTool toolAnnotation = method.getAnnotation(McpTool.class);

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
