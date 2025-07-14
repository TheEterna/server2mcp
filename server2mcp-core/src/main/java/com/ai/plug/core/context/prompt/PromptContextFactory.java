package com.ai.plug.core.context.prompt;


/**
 * @author han
 * @time 2025/7/14 11:46
 */

public class PromptContextFactory {

    /**
     * 获取 PromptContext
     */
    public static IPromptContext createPromptContext() {
        return new PromptContext();
    }
}
