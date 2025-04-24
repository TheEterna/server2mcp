package com.ai.plug.component.parser.des;

import com.ai.plug.component.config.PluginProperties;
import com.ai.plug.component.parser.Parser;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;

/**
 * 需要重新getName
 */
@Component("AbstractParserHandler")
public abstract class AbstractDesParser implements Parser {

    protected AbstractDesParser nextParserHandler;




    /**
     * 是一个模板方法, 省去next逻辑等等
     */
    public String handleParse(Method method, Class<?> toolClass) {

        // 当前这个解析器解析的数据
        String currentHandlerParseData = handleLogic(method, toolClass);

        if (nextParserHandler != null) {
            String nextHandlerParseData = nextParserHandler.handleParse(method, toolClass);
            currentHandlerParseData += nextHandlerParseData;
        }

        return currentHandlerParseData;
    }



    public AbstractDesParser getNextParserHandler() {
        return this.nextParserHandler;
    }

    public AbstractDesParser setNextParserHandler(AbstractDesParser parserHandler) {
        this.nextParserHandler = parserHandler;
        return parserHandler;
    }
}
