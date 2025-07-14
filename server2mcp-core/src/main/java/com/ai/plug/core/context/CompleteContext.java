package com.ai.plug.core.context;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author han
 * @time 2025/6/17 0:53
 */

public class CompleteContext {

    private static Set<String> rawCompletes = ConcurrentHashMap.newKeySet();

    private CompleteContext() {
    }

    public static Set<String> getRawCompletes() {
        return Collections.unmodifiableSet(rawCompletes);
    }

    public static void addComplete(String name) {
        rawCompletes.add(name);
    }




}
