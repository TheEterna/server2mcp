package com.ai.plug.core.context;

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

@Slf4j
public class ResourceContext {

    private static Set<String> rawResources = ConcurrentHashMap.newKeySet();

    private  ResourceContext() {
    }

    public static Set<String> getRawResources() {
        return  Collections.unmodifiableSet(rawResources);
    }

    public static void addResource(String name) {
        rawResources.add(name);
    }






}
