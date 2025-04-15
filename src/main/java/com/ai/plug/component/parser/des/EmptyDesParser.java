package com.ai.plug.component.parser.des;

import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author: 韩
 * time: 2025/03/2025/3/27
 * des:
 */
@Component("EmptyParserHandler")
public class EmptyDesParser extends AbstractDesParser {



    public String getName(){
        return "empty";
    }

    @Override
    public String handleLogic(Method method, Class<?> handlerType) {
        return "";
    }

}
