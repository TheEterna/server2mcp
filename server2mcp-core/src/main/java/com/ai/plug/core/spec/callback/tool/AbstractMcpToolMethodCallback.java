package com.ai.plug.core.spec.callback.tool;

import com.ai.plug.core.spec.utils.elicitation.McpElicitation;
import com.ai.plug.core.spec.utils.elicitation.McpElicitationFactory;
import com.ai.plug.core.spec.utils.logging.McpLogger;
import com.ai.plug.core.spec.utils.logging.McpLoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.logaritex.mcp.annotation.McpArg;
import com.logaritex.mcp.annotation.McpTool;
import com.logaritex.mcp.utils.JsonParser;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Abstract base class for creating callbacks around tool methods.
 *
 * This class provides common functionality for both synchronous and asynchronous tool
 * method callbacks.
 *
 * @author han
 * @time 2025/6/25 11:39
 */
public abstract class AbstractMcpToolMethodCallback {
    protected final Method method;

    protected final Object bean;
    /**
     * The tool name. Unique within the tool set provided to a model.
     */
    protected final String name;

    /**
     * The tool description, used by the AI model to determine what the tool does.
     */
    protected final String description;

    /**
     * The schema of the parameters used to call the tool.
     */
    protected final String inputSchema;
    /**
     * The schema of the result returned by the tool.
     */
    protected final String outputSchema;
    /**
     * The mineType of the result returned by the tool.
     * <p>
     * The mineType is a string that describes the type of data returned by the tool.
     * It is used to determine how the data should be interpreted by the AI model.
     */
    protected final String mineType;
    /**
     * The annotations for the tool.
     */
    protected final McpSchema.ToolAnnotations annotations;

    /**
     *  The converter used to convert the tool method result to a CallToolResult.
     */
    protected final com.ai.plug.core.spec.callback.tool.McpCallToolResultConverter converter;


    /**
     * Constructor for AbstractMcpToolMethodCallback.
     * @param method The method to create a callback for
     * @param bean The bean instance that contains the method
     */
    protected AbstractMcpToolMethodCallback(
            Method method,
            Object bean,
            String name,
            @Nullable String description,
            String inputSchema,
            @Nullable String outputSchema,
            @Nullable String mineType,
            @Nullable McpSchema.ToolAnnotations annotations,
            com.ai.plug.core.spec.callback.tool.McpCallToolResultConverter converter
    ) {
        Assert.notNull(method, "Method can't be null!");
        Assert.notNull(bean, "Bean can't be null!");

        this.method = method;
        this.bean = bean;
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.annotations = annotations;
        this.mineType = mineType;
        this.converter = converter;

        this.validateMethod(this.method);
    }

    /**
     * Validates that the method signature is compatible with the Tool callback.
     * <p>
     * This method checks that the return type is valid and that the parameters match the
     * expected pattern.
     * @param method The method to validate
     * @throws IllegalArgumentException if the method signature is not compatible
     */
    protected void validateMethod(Method method) {
        if (method == null) {
            throw new IllegalArgumentException("Method must not be null");
        }

        // 不需要校验, 也无法校验, 因为你无法确定用户的Result类
        // No need to validate, nor is it possible to validate, because you cannot determine the user's Result class
        this.validateParameters(method);
    }

    /**
     * Exception thrown when there is an error invoking a tool method.
     */
    public static class McpToolMethodException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new exception with the specified detail message and cause.
         * @param message The detail message
         * @param cause The cause
         */
        public McpToolMethodException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new exception with the specified detail message.
         * @param message The detail message
         */
        public McpToolMethodException(String message) {
            super(message);
        }

    }

    /**
     * Validates method parameters. This method provides common validation logic and
     * delegates exchange type checking to subclasses.
     * @param method The method to validate
     * @throws IllegalArgumentException if the parameters are not compatible
     */
    protected void validateParameters(Method method) {
        Parameter[] parameters = method.getParameters();

        // Check for duplicate parameter types
        boolean hasExchangeParam = false;

        for (Parameter param : parameters) {
            Class<?> paramType = param.getType();

            if (isExchangeType(paramType)) {
                if (hasExchangeParam) {
                    throw new IllegalArgumentException("Method cannot have more than one exchange parameter: "
                            + method.getName() + " in " + method.getDeclaringClass().getName());
                }
                hasExchangeParam = true;
            }

        }
    }

    /**
     * Builds the arguments array for invoking the method.
     * <p>
     * This method constructs an array of arguments based on the method's parameter types
     * and the available values (exchange, request).
     * @param method The method to build arguments for
     * @param exchange The server exchange
     * @param arguments The arguments provided by the client
     * @return An array of arguments for the method invocation
     */
    protected Object[] buildArgs(Method method, Object exchange, Map<String, Object> arguments) throws JsonProcessingException {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            if (isExchangeType(paramType)) {
                args[i] = exchange;
            } else if (isLoggerType(paramType)) {
                args[i] = McpLoggerFactory.getLogger(null, exchange, method.getDeclaringClass());
            } else if (isElicitationType(paramType)) {
                args[i] = McpElicitationFactory.getElicitation(exchange);
            }


            else {
                McpArg arg = param.getAnnotation(McpArg.class);
                String paramName = arg != null && !arg.name().isBlank() ? arg.name() : param.getName();

                if (arguments != null && arguments.containsKey(paramName)) {
                    Object argValue = arguments.get(paramName);
                    args[i] = JsonParser.toTypedObject(argValue, paramType);
                }
                else {
                    args[i] = null;
                }
            }
        }

        return args;
    }



    /**
     * Checks if a parameter type is compatible with the exchange type. This method should
     * be implemented by subclasses to handle specific exchange type checking.
     * @param paramType The parameter type to check
     * @return true if the parameter type is compatible with the exchange type, false
     * otherwise
     */
    protected abstract boolean isExchangeType(Class<?> paramType);

    protected boolean isLoggerType(Class<?> paramType) {
        return McpLogger.class.isAssignableFrom(paramType);
    }
    protected boolean isElicitationType(Class<?> paramType) {
        return McpElicitation.class.isAssignableFrom(paramType);
    }


    /**
     * Abstract builder for creating McpToolMethodCallback instances.
     * <p>
     * This builder provides a base for constructing callback instances with the required
     * parameters.
     *
     * @param <T> The type of the builder
     * @param <R> The type of the callback
     */
    @SuppressWarnings("unchecked")
    protected abstract static class AbstractBuilder<T extends AbstractBuilder<T, R>, R> {

        protected Method method;

        protected Object bean;

        protected String name;
        protected String description;
        protected String inputSchema;
        protected String mineType;
        protected String outputSchema;
        protected McpSchema.ToolAnnotations annotations;
        protected com.ai.plug.core.spec.callback.tool.McpCallToolResultConverter converter;
        /**
         * Set the method to create a callback for.
         * @param method The method to create a callback for
         * @return This builder
         */
        public T method(Method method) {
            this.method = method;
            return (T) this;
        }

        public T inputSchema(String inputSchema) {
            this.inputSchema = inputSchema;
            return (T) this;
        }

        public T outputSchema(String outputSchema) {
            this.outputSchema = outputSchema;
            return (T) this;
        }
        public T description(String description) {
            this.description = description;
            return (T) this;
        }

        public T name(String name) {
            this.name = name;
            return (T) this;
        }

        public T mineType(String mineType) {
            this.mineType = mineType;
            return (T) this;
        }

        public T converter(McpCallToolResultConverter converter) {
            this.converter = converter;
            return (T) this;
        }

        public T annotations(McpSchema.ToolAnnotations annotations) {
            this.annotations = annotations;
            return (T) this;
        }

        /**
         * Set the bean instance that contains the method.
         * @param bean The bean instance
         * @return This builder
         */
        public T bean(Object bean) {
            this.bean = bean;
            return (T) this;
        }

        /**
         * Set the Tool annotation.
         * @param tool The Tool annotation
         * @return This builder
         */
        public T toolAnnotation(McpTool tool) {
            if (tool == null) {
                return (T) this;
            }
            this.annotations = new McpSchema.ToolAnnotations(tool.title(),
                    tool.readOnlyHint(),
                    tool.destructiveHint(),
                    tool.idempotentHint(),
                    tool.openWorldHint(),
                    tool.returnDirect());

            return (T) this;
        }

        /**
         * Validate the builder state.
         * @throws IllegalArgumentException if the builder state is invalid
         */
        protected void validate() {
            if (method == null) {
                throw new IllegalArgumentException("Method must not be null");
            }
            if (bean == null) {
                throw new IllegalArgumentException("Bean must not be null");
            }
        }

        /**
         * Build the callback.
         * @return A new callback instance
         */
        public abstract R build();

	}

}
