package com.ai.plug.component.parser.param;

import com.ai.plug.component.config.PluginProperties;

import java.lang.reflect.Method;

/**
 * @ author 韩
 * time: 2025/4/30 2:55
 */

public class MvcParamParser extends AbstractParamAbstractParser {
    @Override
    public String doParse(Method method, Class<?> toolClass) {
        return null;
    }

    @Override
    protected PluginProperties.ParserType getName() {
        return null;
    }

    @Override
    public String doRequiredParse(Method method, Class<?> toolClass) {
        return null;
    }
}
