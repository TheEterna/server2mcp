package com.ai.plug.core.parser.param;

import com.ai.plug.core.parser.AbstractParser;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static com.ai.plug.common.constants.PromptConstants.CUSTOM_PARAM_DES;
import static com.ai.plug.common.constants.PromptConstants.DEFAULT_PARAM_DES;

/**
 * @author: han
 * time: 2025/04/2025/4/1 23:18
 * des:
 */
public abstract class AbstractParamParser extends AbstractParser {

    /**
     * Tool 的 工具的参数 是否必须 (Required) 的 解析逻辑
     * @param toolMethod 工具方法
     * @param toolClass 工具类
     * @param index 索引, 解析具体方法参数 所以需要具体到某个方法的第几个(索引)
     * @return 返回该参数是否必须(Boolean)
     */
    public abstract Boolean doParamRequiredParse(Method toolMethod, Class<?> toolClass, int index);

    /**
     * Tool 默认的 工具的参数 是否必须 (Required) 的 解析逻辑
     * @param toolMethod 工具方法
     * @param toolClass 工具类
     * @param index 索引, 解析具体方法参数 所以需要具体到某个方法的第几个(索引)
     * @return 返回该参数是否必须(Boolean)
     */
    public static Boolean doDefaultParamRequiredParse(Method toolMethod, Class<?> toolClass, int index) {
        Parameter parameter = toolMethod.getParameters()[index];

        // 默认检查是否必须操作
        Nullable nullableAnnotation = parameter.getAnnotation(Nullable.class);

        return nullableAnnotation == null;
    }

    /**
     * Tool 默认的 工具的参数的描述 的解析逻辑
     * @param toolMethod 工具方法
     * @param toolClass 工具类
     * @param index 索引, 解析具体方法参数 所以需要具体到某个方法的第几个(索引)
     * @return 返回执行解析逻辑后的工具参数描述(String)
     */
    public abstract String doParamDesParse(Method toolMethod, Class<?> toolClass, int index);

    /**
     * Tool 默认的 工具的参数的描述 的解析逻辑
     * @param toolMethod 工具方法
     * @param toolClass 工具类
     * @param index 索引, 解析具体方法参数 所以需要具体到某个方法的第几个(索引)
     * @return 返回执行默认解析逻辑后的工具参数描述(String)
     */
    public static String doDefaultParamDesParse(Method toolMethod, Class<?> toolClass, int index) {
        Parameter parameter = toolMethod.getParameters()[index];

        return CUSTOM_PARAM_DES == null ? DEFAULT_PARAM_DES.apply(parameter) :  CUSTOM_PARAM_DES.apply(parameter);
    }


}
