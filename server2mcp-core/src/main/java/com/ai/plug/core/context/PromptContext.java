package com.ai.plug.core.context;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * @author 韩
 * time: 2025/6/13 11:08
 */

@Slf4j
public class PromptContext {

    private static Set<String> rawPrompts = new HashSet<>();

    public PromptContext() {
    }

    public static Set<String> getRawPrompts() {
        return rawPrompts;
    }

    public static void addPrompt(String name) {
        rawPrompts.add(name);
    }





}
