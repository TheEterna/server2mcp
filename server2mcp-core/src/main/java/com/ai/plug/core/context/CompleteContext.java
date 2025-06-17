package com.ai.plug.core.context;

import java.util.HashSet;
import java.util.Set;

/**
 * @author han
 * @time 2025/6/17 0:53
 */

public class CompleteContext {

    private static Set<String> rawCompletes = new HashSet<>();

    public CompleteContext() {
    }

    public static Set<String> getRawCompletes() {
        return rawCompletes;
    }

    public static void addComplete(String name) {
        rawCompletes.add(name);
    }




}
