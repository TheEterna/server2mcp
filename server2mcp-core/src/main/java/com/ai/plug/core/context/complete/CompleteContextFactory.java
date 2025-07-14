package com.ai.plug.core.context.complete;

/**
 * @author han
 * @time 2025/7/14 16:00
 */

public class CompleteContextFactory {
    /**
     * create CompleteContext
     */
    public static ICompleteContext createCompleteContext() {
        return new CompleteContext();
    }
}
