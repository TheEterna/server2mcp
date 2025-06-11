package com.ai.plug.autoconfigure;

import com.ai.plug.autoconfigure.conditional.ConditionalOnParser;
import com.ai.plug.core.parser.des.*;
import com.ai.plug.core.parser.param.*;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * @author 韩
 * time: 2025/5/16 11:40
 */

public class ParserConfig {

    @Bean
    @ConditionalOnParser(value = "TOOL", type = AbstractDesParser.class)
    @Order(0)
    public AbstractDesParser toolDesParser() {
        return new ToolDesParser();
    }

    @Bean
    @ConditionalOnParser(value = "JACKSON", type = AbstractDesParser.class)
    @Order(1)
    public AbstractDesParser jacksonDesParser() {
        return new JacksonDesParser();
    }
    @Bean
    @ConditionalOnParser(value = "JAVADOC", type = AbstractDesParser.class)
    @Order(2)
    public AbstractDesParser javaDocDesParser() {
        return new JavaDocDesParser();
    }
    @Bean
    @ConditionalOnParser(value = "SWAGGER3", type = AbstractDesParser.class)
    @Order(3)
    public AbstractDesParser swagger3DesParser() {
        return new Swagger3DesParser();
    }
    @Bean
    @ConditionalOnParser(value = "SWAGGER2", type = AbstractDesParser.class)
    @Order(4)
    public AbstractDesParser swagger2DesParser() {
        return new Swagger2DesParser();
    }





    @Bean
    @ConditionalOnParser(value = "TOOL", type = AbstractParamParser.class)
    @Order(0)
    public ToolParamParser toolParamParser() {
        return new ToolParamParser();
    }

    @Bean
    @ConditionalOnParser(value = "SPRINGMVC", type = AbstractParamParser.class)
    @Order(1)
    public AbstractParamParser mvcParamParser() {
        return new MvcParamParser();
    }
    @Bean
    @ConditionalOnParser(value = "JACKSON", type = AbstractParamParser.class)
    @Order(2)
    public AbstractParamParser jacksonParamParser() {
        return new JacksonParamParser();
    }

    @Bean
    @ConditionalOnParser(value = "JAVADOC", type = AbstractParamParser.class)
    @Order(3)
    public AbstractParamParser javaDocParamParser() {
        return new JavaDocParamParser();
    }
    @Bean
    @ConditionalOnParser(value = "SWAGGER3", type = AbstractParamParser.class)
    @Order(4)
    public AbstractParamParser swagger3ParamParser() {
        return new Swagger3ParamParser();
    }

    @Bean
    @ConditionalOnParser(value = "SWAGGER2", type = AbstractParamParser.class)
    @Order(5)
    public AbstractParamParser swagger2ParamParser() {
        return new Swagger2ParamParser();
    }







}
