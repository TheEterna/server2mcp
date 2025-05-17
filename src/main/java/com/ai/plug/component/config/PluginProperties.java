package com.ai.plug.component.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;
import static com.ai.plug.common.constants.ConfigConstants.VARIABLE_PREFIX;
import static com.ai.plug.component.config.PluginProperties.ScopeType.INTERFACE;


@ConfigurationProperties(prefix = VARIABLE_PREFIX)
@Component
@Data
public class PluginProperties {

    private boolean enabled = false;
    /**
     * doc
     */
    private Parser parser;

    /**
     * scope, 默认取值 interface 即为扫描接口
     */
    private ScopeType scope = INTERFACE;


    public enum ScopeType {
        INTERFACE,
        CUSTOM;

    }
    public enum DesParserType {
        TOOL, JACKSON, JAVADOC, SWAGGER2, SWAGGER3
    }
    public enum ParamParserType {
        TOOL, JACKSON, SPRINGMVC, JAVADOC, SWAGGER2, SWAGGER3
    }

    @Data
    public static class Parser {
        private List<DesParserType> des;
        private List<ParamParserType> param;
    }

}
