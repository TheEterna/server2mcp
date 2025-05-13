package com.ai.plug.component.parser.starter;

import com.ai.plug.component.parser.des.AbstractDesParser;
import com.ai.plug.component.parser.param.AbstractParamParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;


/**
 * @ author 韩
 * time: 2025/4/29 17:02
 */
@Component
@Slf4j
@Deprecated
public class AddStarter extends Starter {


    @Override
    public String runDesParse(List<AbstractDesParser> parserList, Method method, Class<?> toolClass) {
        StringBuilder result = new StringBuilder();

        parserList.forEach((parser) -> {
            result.append(parser.doDesParse(method, toolClass));
        });

        // 没解析的话，就执行默认操作
        if (!StringUtils.hasText(result.toString())) {
            log.warn("Please note that the parsing module you specified resulted in the {} tool not parsing the tool parameters", toolClass.getName() + "-" + method.getName());
            AbstractDesParser.doDefaultParse(method, toolClass);
        }
        return result.toString();
    }

    @Override
    public Boolean runParamRequiredParse(List<AbstractParamParser> parserList, Method method, Class<?> toolClass, int index) {
        return null;
    }

    @Override
    public String runParamDesParse(List<AbstractParamParser> parserList, Method method, Class<?> toolClass, int index) {
        return null;
    }


}



