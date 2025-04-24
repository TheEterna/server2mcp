package com.ai.plug.component.parser;

import com.ai.plug.component.config.PluginProperties;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;

/**
 * @ author 韩
 * time: 2025/4/23 11:05
 */

public interface Parser {

    String handleLogic(Method method, Class<?> handlerType) ;

    PluginProperties.ParserType getName();
}
