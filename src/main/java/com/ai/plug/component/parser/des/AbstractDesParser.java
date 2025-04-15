package com.ai.plug.component.parser.des;

import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;

/**
 * 需要重新getName
 */
@Component("AbstractParserHandler")
public abstract class AbstractDesParser {

    protected AbstractDesParser nextParserHandler;


    public String getName(){
        return "abstract";
    }


    /**
     * 是一个模板方法, 省去next逻辑等等
     */
    public String handleParse(Method method, Class<?> handlerType) throws FileNotFoundException {

        // 当前这个解析器解析的数据
        String currentHandlerParseData = handleLogic(method, handlerType);

        if (nextParserHandler != null) {
            String nextHandlerParseData = nextParserHandler.handleParse(method, handlerType);
            currentHandlerParseData += nextHandlerParseData;
        }

        return currentHandlerParseData;
    }

    public abstract String handleLogic(Method method, Class<?> handlerType) throws FileNotFoundException;


    public AbstractDesParser getNextParserHandler() {

        return this.nextParserHandler;
    }

    public void setNextParserHandler(AbstractDesParser parserHandler) {
        this.nextParserHandler = parserHandler;
    }
}
