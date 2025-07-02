package com.ai.plug.core.spec.callback.tool;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author han
 * @time 2025/6/25 22:28
 */

public class SyncMcpToolMethodCallback extends AbstractMcpToolMethodCallback
        implements BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult>  {

    private final Logger logger = LoggerFactory.getLogger(SyncMcpToolMethodCallback.class);

    private SyncMcpToolMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.name, builder.description, builder.inputSchema,
                builder.outputSchema, builder.mineType, builder.annotations, builder.converter);
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
        return McpSyncServerExchange.class.isAssignableFrom(paramType);
    }

    /**
     * Applies this function to the given arguments.
     *
     * @param exchange the first function argument
     * @param arguments the second function argument
     * @return the function result
     */
    @Override
    public McpSchema.CallToolResult apply(McpSyncServerExchange exchange, Map<String, Object> arguments) {

        try {
            // Build arguments for the method call
            Object[] args = this.buildArgs(this.method, exchange, arguments);

            // Invoke the method
            Object result = this.callMethod(args);

            logger.debug("Successful execution of tool: {}", this.name);

            if (result instanceof Mono<?>) {
                // If the result is already a Mono, map it to a GetPromptResult
                result = ((Mono<?>) result).block();
            }
            // Get the return type of the method
            Type returnType = this.method.getGenericReturnType();

            // Convert the result to a GetPromptResult
            return this.converter.convertToCallToolResult(result, returnType, this);
        }
        catch (Exception e) {
            throw new McpToolMethodException("Error invoking prompt method: " + this.method.getName(), e);
        }
    }




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

    @Override
    public String toString() {
        return "SyncMcpToolMethodCallback{" +
                "method=" + method +
                ", bean=" + bean +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", inputSchema='" + inputSchema + '\'' +
                ", outputSchema='" + outputSchema + '\'' +
                ", annotations='" + annotations + '\'' +
                '}';
    }


    public static class Builder extends AbstractBuilder<Builder, SyncMcpToolMethodCallback> {

        /**
         * Build the callback.
         * @return A new AsyncMcpResourceMethodCallback instance
         */
        @Override
        public SyncMcpToolMethodCallback build() {
            validate();
            return new SyncMcpToolMethodCallback(this);
        }

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

}
