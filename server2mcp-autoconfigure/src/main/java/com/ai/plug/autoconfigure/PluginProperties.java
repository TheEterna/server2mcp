package com.ai.plug.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ai.plug.common.constants.ConfigConstants.VARIABLE_PREFIX;

/**
 * @author 韩
 */

@ConfigurationProperties(prefix = VARIABLE_PREFIX)
@Component
@Data
public class PluginProperties {

    private boolean enabled = false;
    /**
     * doc
     */
    private Parser parser;

    private Resource resource;

    /**
     * scope, 默认取值 interface 即为扫描接口
     */
    private ScopeType scope = ScopeType.INTERFACE;


    /**
     * 启动时扫描范围
     * INTERFACE 为 启动时 自动扫描注册接口工具
     * CUSTOM 为 啥也不干
     */
    public enum ScopeType {
        INTERFACE,
        CUSTOM;

    }

    /**
     * 工具描述解析器枚举
     * @Field TOOL
     * @Field JACKSON
     * @Field JAVADOC
     * @Field SWAGGER2
     * @Field SWAGGER3
     */
    public enum DesParserType {
        /**
         * Spring-ai 原生Tool系列注解解析
         */
        TOOL,
        /**
         * JACKSON系列注解解析
         */
        JACKSON,
        /**
         * JAVADOC解析, 需要特定条件,源码或 构建时同时构建源代码
         */
        JAVADOC,
        /**
         * SWAGGER2系列注解解析
         */
        SWAGGER2,
        /**
         * SWAGGER3系列注解解析
         */
        SWAGGER3
    }
    /**
     * 工具参数解析器枚举
     * @Field TOOL
     * @Field JACKSON
     * @Field JAVADOC
     * @Field SWAGGER2
     * @Field SWAGGER3
     */
    public enum ParamParserType {
        /**
         * Spring-ai 原生Tool系列注解解析
         */
        TOOL,
        /**
         * JACKSON系列注解解析
         */
        JACKSON,
        /**
         * SPRINGMVC系列注解解析
         */
        SPRINGMVC,
        /**
         * JAVADOC解析, 需要特定条件,源码或 构建时同时构建源代码
         */
        JAVADOC,
        /**
         * SWAGGER2系列注解解析
         */
        SWAGGER2,
        /**
         * SWAGGER3系列注解解析
         */
        SWAGGER3
    }

    @Data
    public static class Parser {
        private List<DesParserType> des;
        private List<ParamParserType> param;
    }

    @Data
    private static class Resource {
        private boolean enabled = true;
    }

}
