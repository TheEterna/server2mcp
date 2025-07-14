package com.ai.plug.core.context.prompt;

import java.util.Set;

/**
 * @author han
 * @time 2025/7/14 11:34
 */

public interface IPromptContext {

    /**
     * 获取原始的prompt
     */
    Set<String> getRawPrompts();


    /**
     * 添加prompt
     */
    void addPrompt(String name);

}
