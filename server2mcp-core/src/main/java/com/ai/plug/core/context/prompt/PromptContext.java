package com.ai.plug.core.context.prompt;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 韩
 * time: 2025/6/13 11:08
 */

@Slf4j
public class PromptContext implements IPromptContext{

    private Set<String> rawPrompts = ConcurrentHashMap.newKeySet();

    PromptContext() {
    }

    @Override
    public Set<String> getRawPrompts() {
        return rawPrompts;
    }

    @Override
    public void addPrompt(String name) {
        rawPrompts.add(name);
    }





}
