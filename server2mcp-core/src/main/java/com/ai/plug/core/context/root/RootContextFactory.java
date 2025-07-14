package com.ai.plug.core.context.root;

/**
 * @author han
 * @time 2025/7/14 1:50
 */

public final class RootContextFactory {
    /**
     * create RootContext
     */
    public static IRootContext createRootContext() {
        return new RootContext();
    }
}
