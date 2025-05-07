package com.ai.plug.component.parser.des;

import com.ai.plug.component.parser.AbstractParser;
import org.springframework.ai.util.ParsingUtils;

import java.lang.reflect.Method;

/**
 * 需要重新getName
 */

public abstract class AbstractDesParser extends AbstractParser {


    public static String doDefaultParse(Method toolMethod, Class<?> toolClass) {
        return ParsingUtils.reConcatenateCamelCase(toolMethod.getName(), " ");
    }

    public abstract String doDesParse(Method method, Class<?> toolClass) ;


}
