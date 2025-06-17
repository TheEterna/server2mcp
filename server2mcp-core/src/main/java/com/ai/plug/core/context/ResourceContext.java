package com.ai.plug.core.context;

import com.ai.plug.core.annotation.McpResourceScan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author 韩
 * time: 2025/6/4 15:14
 */

@Slf4j
public class ResourceContext {

    private static Set<String> rawResources = new HashSet<>();

    public ResourceContext() {
    }

    public static Set<String> getRawResources() {
        return rawResources;
    }

    public static void addResource(String name) {
        rawResources.add(name);
    }






}
