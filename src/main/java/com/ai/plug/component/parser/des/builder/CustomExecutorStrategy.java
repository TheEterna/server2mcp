package com.ai.plug.component.parser.des.builder;

import com.ai.plug.component.config.PluginProperties;
import com.ai.plug.component.parser.des.AbstractDesParser;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author: 韩
 * time: 2025/3/26
 * des:
 */

public class CustomExecutorStrategy extends ExecutorStrategy {



    public CustomExecutorStrategy(PluginProperties pluginProperties, List<AbstractDesParser> concreteParserHandlers) {
        super(pluginProperties, concreteParserHandlers);
    }

    protected void handle() {

//        handleCore();
       // 因为无法多继承, 需要把操作转移到 HandlerMapping 类中
    }

    // 根据具体业务, 自行实现
    public String handleCore(Method method, Class<?> handlerType) throws FileNotFoundException {
        // 把这个类解析成mcp的tool数据, 应该在这里处理异常, 但是由于handlerMapping由MVC实现, 没有调用权限, 只能牺牲一些清晰性了
        return header.handleParse(method, handlerType);
    }

}
