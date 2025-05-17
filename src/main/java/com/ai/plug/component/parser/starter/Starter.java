package com.ai.plug.component.parser.starter;

import com.ai.plug.component.parser.des.AbstractDesParser;
import com.ai.plug.component.parser.param.AbstractParamParser;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @ author 韩
 * time: 2025/4/29 17:02
 */

public abstract class Starter {
    public abstract String runDesParse(List<AbstractDesParser> parserList, Method method, Class<?> toolClass);

    public abstract Boolean runParamRequiredParse(List<AbstractParamParser> parserList, Method method, Class<?> toolClass, int index);

    public abstract String runParamDesParse(List<AbstractParamParser> parserList, Method method, Class<?> toolClass, int index);

}
