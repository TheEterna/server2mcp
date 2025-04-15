package com.ai.plug.component.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.ai.plug.common.constants.ConfigConstants.VARIABLE_PREFIX;
import static com.ai.plug.component.config.PluginProperties.ParserType.JAVADOC;
import static com.ai.plug.component.config.PluginProperties.ScopeType.INTERFACE;


@ConfigurationProperties(prefix = VARIABLE_PREFIX)
@Component
@Data
public class PluginProperties {
    private boolean enabled = false;

    /**
     * 实现方式，
     * mvc
     * ...
     */
//    private String impl = "mvc";

    /**
     * doc
     */
    private List<ParserType> parser = Arrays.asList(JAVADOC);

    /**
     * scope, 默认取值 interface 即为扫描接口
     */
    private ScopeType scope = INTERFACE;


    public static enum ScopeType {
        INTERFACE,
        CUSTOM;

        private ScopeType() {
        }
    }
    public static enum ParserType {
        JAVADOC;

        private ParserType() {
        }
    }

}