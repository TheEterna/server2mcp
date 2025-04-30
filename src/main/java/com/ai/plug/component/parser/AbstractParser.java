package com.ai.plug.component.parser;

import com.ai.plug.component.config.PluginProperties;

import java.lang.reflect.Method;

/**
 * @ author 韩
 * time: 2025/4/23 11:05
 */

public abstract class AbstractParser {


    public abstract String doParse(Method method, Class<?> toolClass) ;

    protected abstract PluginProperties.ParserType getName();



}
