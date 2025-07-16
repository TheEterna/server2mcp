package com.ai.plug.core.context.resource;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 韩
 * time: 2025/6/4 15:14
 */

public class ResourceContext implements IResourceContext{

    private static Set<String> rawResources = ConcurrentHashMap.newKeySet();

    ResourceContext() {
    }
    /**
     * 获取 未处理的Resources
     */
    @Override
    public Set<String> getRawResources() {
        return  Collections.unmodifiableSet(rawResources);
    }

    /**
     * 添加 Resource
     * add Resource
     */
    @Override
    public void addResource(String name) {
        rawResources.add(name);
    }



}
