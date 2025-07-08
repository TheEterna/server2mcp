package com.ai.plug.core.parser.tool.starter;

import com.ai.plug.core.parser.tool.des.AbstractDesParser;
import com.ai.plug.core.parser.tool.param.AbstractParamParser;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 整体逻辑: 每个解析器 解析出来的 数据是一定的, starter 作为一个掌控者, 对解析器 进行编排
 * time: 2025/4/29 17:02
 * @author 她也曾美如画
 */

public abstract class AbstractStarter {


    /**
     * 编排解析工具描述的解析逻辑
     * @param parserList 解析器列表
     * @param toolMethod 工具的方法
     * @param toolClass 工具的类
     * @return 解析后的工具注释
     */
    public abstract String runDesParse(List<AbstractDesParser> parserList, Method toolMethod, Class<?> toolClass);

    /**
     * 解析工具参数是否必须
     * @param parserList 解析器列表
     * @param toolMethod 工具的方法
     * @param toolClass 工具的类
     * @param index 参数 索引
     * @return 工具参数是否必须
     */
    public abstract Boolean runParamRequiredParse(List<AbstractParamParser> parserList, Method toolMethod, Class<?> toolClass, int index);

    /**
     * 解析工具参数的描述
     * @param parserList 解析器列表
     * @param toolMethod 工具的方法
     * @param toolClass 工具的类
     * @param index 参数 索引
     * @return 工具参数的描述
     */
    public abstract String runParamDesParse(List<AbstractParamParser> parserList, Method toolMethod, Class<?> toolClass, int index);

}
