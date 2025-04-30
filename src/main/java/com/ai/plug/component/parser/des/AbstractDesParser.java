package com.ai.plug.component.parser.des;

import com.ai.plug.component.parser.AbstractParser;
import org.springframework.ai.util.ParsingUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 需要重新getName
 */

public abstract class AbstractDesParser extends AbstractParser {


    public static String doDefaultParse(Method toolMethod, Class<?> toolClass) {
        return ParsingUtils.reConcatenateCamelCase(toolMethod.getName(), " ");
    }



}
