package com.ai.plug.core.parser.des;

import com.ai.plug.core.parser.AbstractParser;
import org.springframework.ai.util.ParsingUtils;

import java.lang.reflect.Method;

/**
 * @author 韩
 * 需要重新getName
 */
public abstract class AbstractDesParser extends AbstractParser {

    /**
     * 默认Tool 的 工具描述(description)解析逻辑
     * @param toolMethod 工具方法
     * @param toolClass 工具类
     * @return 返回执行默认解析逻辑后的工具描述(String)
     */
    public static String doDefaultParse(Method toolMethod, Class<?> toolClass) {
        return ParsingUtils.reConcatenateCamelCase(toolMethod.getName(), " ");
    }
    /**
     * Tool 的 工具描述(description)解析逻辑, 供子类重写
     * @param method 工具方法
     * @param toolClass 工具类
     * @return 返回执行解析逻辑后的工具描述(String)
     */
    public abstract String doDesParse(Method method, Class<?> toolClass) ;


}
