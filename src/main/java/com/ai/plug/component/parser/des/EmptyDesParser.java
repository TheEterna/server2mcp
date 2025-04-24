package com.ai.plug.component.parser.des;

import com.ai.plug.component.config.PluginProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author: 韩
 * time: 2025/03/2025/3/27
 * des:
 */
@Component("EmptyDesParser")
public class EmptyDesParser extends AbstractDesParser {



    public PluginProperties.ParserType getName(){
        return null;
    }

    @Override
    public String handleLogic(Method method, Class<?> handlerType) {
        return "";
    }

}
