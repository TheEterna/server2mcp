package com.ai.plug.core.context.resource;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

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
