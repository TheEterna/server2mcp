package com.ai.plug.component.parser.aggregater;

import com.ai.plug.component.config.PluginProperties;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @ author 韩
 * time: 2025/4/23 10:30
 */

public abstract class AbstractParserAggregater {

    @Autowired
    protected PluginProperties pluginProperties;

    public AbstractParserAggregater() {
    }
}
