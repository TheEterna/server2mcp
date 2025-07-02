package com.ai.plug.core.provider;

import com.ai.plug.core.spec.callback.resource.AsyncMcpResourceMethodCallback;
import com.ai.plug.core.spec.callback.resource.SyncMcpResourceMethodCallback;
import com.logaritex.mcp.annotation.McpResource;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author han
 * @time 2025/6/19 17:48
 */

public class McpResourceProvider {
    private final List<Object> resourceObjects;

    public McpResourceProvider(List<Object> resourceObjects) {
        Assert.notNull(resourceObjects, "resourceObjects cannot be null");
        this.resourceObjects = resourceObjects;
	}

    /**
     * Returns a list of resource specifications for async resource methods.
     * @return List<McpServerFeatures.AsyncResourceSpecification>
     */
    public List<McpServerFeatures.AsyncResourceSpecification> getAsyncResourceSpecifications() {

        List<McpServerFeatures.AsyncResourceSpecification> methodCallbacks = this.resourceObjects.stream()
                .map(resourceObject -> Stream.of(doGetClassMethods(resourceObject))
                        .filter(resourceMethod -> resourceMethod.isAnnotationPresent(McpResource.class))
                        .map(mcpResourceMethod -> {
                            McpResource resourceAnnotation = mcpResourceMethod.getAnnotation(McpResource.class);

                            String uri = resourceAnnotation.uri();
                            String name = getName(mcpResourceMethod, resourceAnnotation);
                            String description = resourceAnnotation.description();
                            String mimeType = resourceAnnotation.mimeType();
                            var mcpResource = new McpSchema.Resource(uri, name, description, mimeType, null);

                            AsyncMcpResourceMethodCallback methodCallback = AsyncMcpResourceMethodCallback.builder()
                                    .method(mcpResourceMethod)
                                    .bean(resourceObject)
                                    .resource(mcpResource)
                                    .build();

                            return new McpServerFeatures.AsyncResourceSpecification(mcpResource, methodCallback);
                        })
                        .toList())
                .flatMap(List::stream)
                .toList();

        return methodCallbacks;
    }

    /**
     * Returns a list of resource specifications for sync resource methods.
     * @return List<McpServerFeatures.SyncResourceSpecification>
     */
    public List<McpServerFeatures.SyncResourceSpecification> getSyncResourceSpecifications() {

        List<McpServerFeatures.SyncResourceSpecification> methodCallbacks = this.resourceObjects.stream()
                .map(resourceObject -> Stream.of(doGetClassMethods(resourceObject))
                        .filter(resourceMethod -> resourceMethod.isAnnotationPresent(McpResource.class))
                        .map(mcpResourceMethod -> {
                            McpResource resourceAnnotation = mcpResourceMethod.getAnnotation(McpResource.class);

                            String uri = resourceAnnotation.uri();
                            String name = getName(mcpResourceMethod, resourceAnnotation);
                            String description = resourceAnnotation.description();
                            String mimeType = resourceAnnotation.mimeType();
                            var mcpResource = new McpSchema.Resource(uri, name, description, mimeType,null, null);

                            SyncMcpResourceMethodCallback methodCallback = SyncMcpResourceMethodCallback.builder()
                                    .method(mcpResourceMethod)
                                    .bean(resourceObject)
                                    .resource(mcpResource)
                                    .build();

                            return new McpServerFeatures.SyncResourceSpecification(mcpResource, methodCallback);
                        })
                        .toList())
                .flatMap(List::stream)
                .toList();

        return methodCallbacks;
    }

    /**
     * Returns the methods of the given bean class.
     * @param bean the bean instance
     * @return the methods of the bean class
     */
    protected Method[] doGetClassMethods(Object bean) {
        Method[] methods = bean.getClass().getDeclaredMethods();
        Arrays.sort(methods, Comparator
                .comparing(Method::getName)
                .thenComparing(method -> Arrays.toString(method.getParameterTypes())));
        return methods;
    }

    private static String getName(Method method, McpResource resource) {
        Assert.notNull(method, "method cannot be null");
        if (resource == null || resource.name() == null || resource.name().isEmpty()) {
            return method.getName();
        }
        return resource.name();
	}


}
