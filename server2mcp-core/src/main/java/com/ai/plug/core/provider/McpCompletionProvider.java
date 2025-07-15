package com.ai.plug.core.provider;

import com.ai.plug.core.spec.callback.complete.AsyncMcpCompleteMethodCallback;
import com.ai.plug.core.spec.callback.complete.SyncMcpCompleteMethodCallback;
import com.ai.plug.core.annotation.CompleteAdapter;
import com.ai.plug.core.annotation.McpComplete;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.util.Assert;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author han
 * @time 2025/6/19 18:09
 */

public class McpCompletionProvider {
    private final List<Object> completeObjects;

    public McpCompletionProvider(List<Object> completeObjects) {
        Assert.notNull(completeObjects, "completeObjects cannot be null");
        this.completeObjects = completeObjects;
    }

    /**
     * get sync complete specification
     * @return List<McpServerFeatures.SyncCompletionSpecification>
     */
    public List<McpServerFeatures.SyncCompletionSpecification> getSyncCompleteSpecifications() {

        List<McpServerFeatures.SyncCompletionSpecification> syncCompleteSpecification = this.completeObjects.stream()
                .map(completeObject -> Stream.of(doGetClassMethods(completeObject))
                        .filter(method -> method.isAnnotationPresent(McpComplete.class))
                        .filter(method -> !Mono.class.isAssignableFrom(method.getReturnType()))
                        .map(mcpCompleteMethod -> {
                            var completeAnnotation = mcpCompleteMethod.getAnnotation(McpComplete.class);
                            var completeRef = CompleteAdapter.asCompleteReference(completeAnnotation, mcpCompleteMethod);

                            SyncMcpCompleteMethodCallback methodCallback = SyncMcpCompleteMethodCallback.builder()
                                    .method(mcpCompleteMethod)
                                    .bean(completeObject)
                                    .reference(completeRef)
                                    .build();

                            return new McpServerFeatures.SyncCompletionSpecification(completeRef, methodCallback);
                        })
                        .toList())
                .flatMap(List::stream)
                .toList();

        return syncCompleteSpecification;
    }

    /**
     * get async complete specification
     * @return List<McpServerFeatures.AsyncCompletionSpecification>
     */
    public List<McpServerFeatures.AsyncCompletionSpecification> getAsyncCompleteSpecifications() {

        List<McpServerFeatures.AsyncCompletionSpecification> asyncCompleteSpecification = this.completeObjects.stream()
                .map(completeObject -> Stream.of(doGetClassMethods(completeObject))
                        .filter(method -> method.isAnnotationPresent(McpComplete.class))
                        .filter(method -> !Mono.class.isAssignableFrom(method.getReturnType()))
                        .map(mcpCompleteMethod -> {
                            var completeAnnotation = mcpCompleteMethod.getAnnotation(McpComplete.class);
                            var completeRef = CompleteAdapter.asCompleteReference(completeAnnotation, mcpCompleteMethod);

                            AsyncMcpCompleteMethodCallback methodCallback = AsyncMcpCompleteMethodCallback.builder()
                                    .method(mcpCompleteMethod)
                                    .bean(completeObject)
                                    .reference(completeRef)
                                    .build();

                            return new McpServerFeatures.AsyncCompletionSpecification(completeRef, methodCallback);
                        })
                        .toList())
                .flatMap(List::stream)
                .toList();

        return asyncCompleteSpecification;
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

}
