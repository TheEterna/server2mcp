package com.ai.plug.core.context.resource;

import java.util.Set;

/**
 * @author han
 * @time 2025/7/14 11:34
 */

public interface IResourceContext {
    /**
     * 获取 未处理的Resources
     */
    Set<String> getRawResources();

    /**
     * 添加 Resource
     * add Resource
     */
    void addResource(String name);
}
