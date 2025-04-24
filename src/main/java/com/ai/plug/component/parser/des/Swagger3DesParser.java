package com.ai.plug.component.parser.des;

import com.ai.plug.component.config.PluginProperties;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

import static com.ai.plug.common.constants.ConfigConstants.PARSER_PREFIX;
import static com.ai.plug.common.constants.ConfigConstants.VARIABLE_PREFIX;
import static com.ai.plug.component.config.PluginProperties.ParserType.SWAGGER2;
import static com.ai.plug.component.config.PluginProperties.ParserType.SWAGGER3;

/**
 * @ author 韩
 * time: 2025/4/24 3:43
 */

@Component("Swagger2DesParser")
@ConditionalOnProperty(prefix = VARIABLE_PREFIX, name = PARSER_PREFIX, havingValue = "SWAGGER3")
public class Swagger3DesParser extends AbstractDesParser {
    @Override
    public PluginProperties.ParserType getName(){
        return SWAGGER3;
    }

    @Override
    public String handleLogic(Method method, Class<?> toolClass) {
        String result = "";

        Operation operationAnnotation = method.getAnnotation(Operation.class);

        String simpleDes = operationAnnotation.summary();

        String detailedDes = operationAnnotation.description();

        // 如果有值
        if (StringUtils.hasText(simpleDes)) {
            result += simpleDes;
        }

        if (StringUtils.hasText(detailedDes)) {
            // 如果之前已经有了一个简单描述了, 就加一个回车
            if (StringUtils.hasText(result)) {
                result += '\n';
            }
            result += detailedDes;
        }

        // todo 可以加一个判断如果过时 就一句 (已过时) 提示词

        return result;
    }
}
