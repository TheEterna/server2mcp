package com.ai.plug.core.context.resource;

/**
 * @author han
 * @time 2025/7/14 16:00
 */

public class ResourceContextFactory {
    /**
     * create ResourceContext
     */
    public static IResourceContext createResourceContext() {
        return new ResourceContext();
    }
}
