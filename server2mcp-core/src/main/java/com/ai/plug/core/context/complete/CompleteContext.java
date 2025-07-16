package com.ai.plug.core.context.complete;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author han
 * @time 2025/6/17 0:53
 */

public class CompleteContext implements ICompleteContext{

    private static Set<String> rawCompletes = ConcurrentHashMap.newKeySet();

    CompleteContext() {
    }

    @Override
    public Set<String> getRawCompletes() {
        return Collections.unmodifiableSet(rawCompletes);
    }

    @Override
    public void addComplete(String name) {
        rawCompletes.add(name);
    }




}
