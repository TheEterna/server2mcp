package com.ai.plug.core.parser.starter;

import com.ai.plug.core.parser.des.AbstractDesParser;
import com.ai.plug.core.parser.param.AbstractParamParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;


/**
 * @author 韩
 * time: 2025/4/29 17:03
 */
@Slf4j
public final class SingleStarter extends AbstractStarter {


    @Override
    public String runDesParse(List<AbstractDesParser> parserList, Method method, Class<?> toolClass) {

        // 找到 tool 的描述就不在解析了
        String result = "";
        for (AbstractDesParser parser : parserList) {
            result = parser.doDesParse(method, toolClass);
            if (StringUtils.hasText(result)) {
                break;
            }
        }

        if (!StringUtils.hasText(result)) {
            log.warn("Please note that the parsing module you specified resulted in the {} tool not parsing the tool description", toolClass.getName() + "-" + method.getName());
            return AbstractDesParser.doDefaultParse(method, toolClass);
        }
        return result;

    }

    @Override
    public Boolean runParamRequiredParse(List<AbstractParamParser> parserList, Method method, Class<?> toolClass, int index) {

        Boolean result = parserList.stream().map((parser) -> {
            return parser.doParamRequiredParse(method, toolClass, index);
        }).filter(Objects::nonNull).findFirst().orElse(null);

        if (result == null) {
            log.warn("Please note that the parsing module you specified resulted in the {} tool not parsing the param required", toolClass.getName() + "-" + method.getName());
            return AbstractParamParser.doDefaultParamRequiredParse(method, toolClass, index);
        }
        return result;
    }

    @Override
    public String runParamDesParse(List<AbstractParamParser> parserList, Method method, Class<?> toolClass, int index) {
        String result = parserList.stream().map((parser) -> {
            return parser.doParamDesParse(method, toolClass, index);
        }).filter(Objects::nonNull).findFirst().orElse(null);

        if (result == null) {
            log.warn("Please note that the parsing module you specified resulted in the {} tool not parsing the param required", toolClass.getName() + "-" + method.getName());
            return AbstractParamParser.doDefaultParamDesParse(method, toolClass, index);
        }

        return result;

    }


}
