package com.ai.plug.core.spec.callback.tool;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.*;
import reactor.util.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiFunction;

/**
 *
 * @author han
 * @time 2025/6/25 22:31
 */

public class AsyncMcpToolMethodCallback extends AbstractMcpToolMethodCallback
//        implements BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> {
        implements BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> {

    private final static Logger logger = LoggerFactory.getLogger(AsyncMcpToolMethodCallback.class);

    private AsyncMcpToolMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.name, builder.description, builder.inputSchema,
                builder.outputSchema, builder.mineType, builder.annotations, builder.toolAnnotation,
                builder.idempotentCache, builder.converter, builder.rootContext);
        this.validateMethod(this.method);
    }

    /**
     * Checks if a parameter type is compatible with the exchange type. This method should
     * be implemented by subclasses to handle specific exchange type checking.
     *
     * @param paramType The parameter type to check
     * @return true if the parameter type is compatible with the exchange type, false
     * otherwise
     */
    @Override
    protected boolean isExchangeType(Class<?> paramType) {
        return McpAsyncServerExchange.class.isAssignableFrom(paramType);
    }

    /**
     * Applies this function to the given arguments.
     *
     * @param exchange Server exchange
//     * @param arguments the arguments of tool calling  (deprecated)
     * @param callToolRequest the tool Calling pojo
     * @return the function result
     */
    @Override
    public Mono<McpSchema.CallToolResult> apply(McpAsyncServerExchange exchange, McpSchema.CallToolRequest callToolRequest) {
        return Mono.fromCallable(() -> {
                // Build arguments for the method call
                Object[] args = super.buildArgs(this.method, exchange, callToolRequest.arguments(), callToolRequest);

                // Invoke the method
                Object result = this.callMethod(args);

                logger.debug("Successful execution of tool: {}", this.name);

                // Get the return type of the method
                Type returnType = this.method.getGenericReturnType();

                // Convert the result to a GetPromptResult
                return this.converter.convertToCallToolResult(result, returnType, this);
            }).doOnError(e ->
                logger.error("Error invoking tool method: {}", this.method.getName(), e)
            )
            .onErrorMap(e ->
                new McpToolMethodException("Error invoking tool method: " + this.method.getName(), e)
            )
            .subscribeOn(Schedulers.boundedElastic());
    }

//    @Override
//    @Deprecated
//    public Mono<McpSchema.CallToolResult> apply(McpAsyncServerExchange exchange, Map<String, Object> arguments) {
//        return Mono.defer(() -> {
//            try {
//                // Build arguments for the method call
//                Object[] args = super.buildArgs(this.method, exchange, arguments);
//
//                // Invoke the method
//                Object result = this.callMethod(args);
//
//                logger.debug("Successful execution of tool: {}", this.name);
//
//                // Get the return type of the method
//                Type returnType = this.method.getGenericReturnType();
//
//                // Convert the result to a GetPromptResult
//                return Mono.just(this.converter.convertToCallToolResult(result, returnType, this));
//
//            } catch (Exception e) {
//                logger.error("Error invoking prompt method: {}", this.method.getName(), e);
//                return Mono.error(
//                        new McpToolMethodException("Error invoking prompt method: " + this.method.getName(), e));
//            }
//        });
//    }


    @Nullable
    private Object callMethod(Object[] methodArguments) {
        if (isObjectNotPublic() || isMethodNotPublic()) {
            this.method.setAccessible(true);
        }

        Object result;
        try {
            result = this.method.invoke(this.bean, methodArguments);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
        }
        catch (InvocationTargetException ex) {
            throw new McpToolMethodException(this.toString(), ex.getCause());
        }
        return result;
    }

    private boolean isObjectNotPublic() {
        return this.bean != null && !Modifier.isPublic(this.bean.getClass().getModifiers());
    }

    private boolean isMethodNotPublic() {
        return !Modifier.isPublic(this.method.getModifiers());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder, AsyncMcpToolMethodCallback> {

        /**
         * Set the dedup cache — overrides the no-op default in AbstractBuilder.
         */
        @Override
        public Builder idempotentCache(@Nullable com.ai.plug.core.spec.dedup.IdempotentCache cache) {
            this.idempotentCache = cache;
            return this;
        }

        /**
         * Build the callback.
         * @return A new AsyncMcpToolMethodCallback instance
         */
        @Override
        public AsyncMcpToolMethodCallback build() {
            validate();
            return new AsyncMcpToolMethodCallback(this);
        }

    }

}
