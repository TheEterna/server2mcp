package com.ai.plug.core.spec.callback.tool;

import com.ai.plug.common.utils.JsonParser;
import com.ai.plug.core.annotation.McpArg;
import com.ai.plug.core.annotation.McpTool;
import com.ai.plug.core.context.root.IRootContext;
import com.ai.plug.core.spec.utils.elicitation.McpElicitation;
import com.ai.plug.core.spec.utils.elicitation.McpElicitationFactory;
import com.ai.plug.core.spec.utils.logging.McpLogger;
import com.ai.plug.core.spec.utils.logging.McpLoggerFactory;
import com.ai.plug.core.spec.utils.root.McpRoot;
import com.ai.plug.core.spec.utils.root.McpRootFactory;
import com.ai.plug.core.spec.utils.sampling.McpSampling;
import com.ai.plug.core.spec.utils.sampling.McpSamplingFactory;
import com.ai.plug.core.spec.utils.progress.McpProgress;
import com.ai.plug.core.spec.utils.progress.McpProgressFactory;
import com.ai.plug.core.spec.pagination.McpPaging;
import com.ai.plug.core.spec.request.McpRequestId;
import com.ai.plug.core.spec.dedup.IdempotentCache;
import io.modelcontextprotocol.spec.McpSchema;
import com.ai.plug.core.utils.CustomToolUtil;
import tools.jackson.core.JacksonException;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;
import lombok.extern.slf4j.*;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
@Slf4j
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
     * The original {@link McpTool} annotation on the method. Kept so the result
     * converter can read resultType / ttlMs / cacheScope wire-layer hints
     * (MCP protocol 2026-07-28). Null when the method is not annotated.
     */
    @Nullable
    protected final McpTool toolAnnotation;

    /**
     *  The converter used to convert the tool method result to a CallToolResult.
     */
    protected final com.ai.plug.core.spec.callback.tool.McpCallToolResultConverter converter;

    /**
     * Optional dedup cache — when set AND {@code @McpTool.idempotentHint=true},
     * the callback returns the cached {@link McpSchema.CallToolResult} for
     * repeated invocations within the cache's TTL window instead of re-running
     * the underlying method. Null by default; assign at construction time
     * via {@link AbstractBuilder#idempotentCache(IdempotentCache)}.
     */
    @Nullable
    protected IdempotentCache idempotentCache;

    protected IRootContext rootContext;

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
            @Nullable McpTool toolAnnotation,
            @Nullable com.ai.plug.core.spec.dedup.IdempotentCache idempotentCache,
            com.ai.plug.core.spec.callback.tool.McpCallToolResultConverter converter,
            IRootContext rootContext
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
        this.toolAnnotation = toolAnnotation;
        this.mineType = mineType;
        this.idempotentCache = idempotentCache;
        this.converter = converter;
        this.rootContext = rootContext;
        this.rootContext = rootContext;

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
    protected Object[] buildArgs(Method method, Object exchange, Map<String, Object> arguments,
                                  @Nullable McpSchema.CallToolRequest request)
            throws JacksonException {
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
            } else if (isSamplingType(paramType)) {
                args[i] = McpSamplingFactory.getSampling(exchange);
            } else if (isRootType(paramType)) {
                args[i] = McpRootFactory.getRoot(exchange);
            } else if (isProgressType(paramType)) {
                args[i] = McpProgressFactory.getProgress(exchange, request);
            } else if (isPagingType(paramType)) {
                McpPaging paging = McpPaging.fromCursor(
                    extractMetaString(request, "cursor"),
                    extractMetaInt(request, "pageSize"));
                args[i] = paging;
                // Track for the converter's auto-slicing logic
                capturedPaging = paging;
            } else if (isRequestIdType(paramType)) {
                args[i] = McpRequestId.of(extractMetaString(request, "requestId"));
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

        // Root Injection Point
//        try {
//            CustomToolUtil.mcpInjection(exchange, this.rootContext);
//        } catch (Exception e) {
//            log.error("Error injecting roots into exchange", e);
//        }

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

    protected boolean isSamplingType(Class<?> paramType) {
        return McpSampling.class.isAssignableFrom(paramType);
    }
    protected boolean isRootType(Class<?> paramType) {
        return McpRoot.class.isAssignableFrom(paramType);
    }

    protected boolean isProgressType(Class<?> paramType) {
        return McpProgress.class.isAssignableFrom(paramType);
    }

    protected boolean isPagingType(Class<?> paramType) {
        return McpPaging.class.isAssignableFrom(paramType);
    }

    protected boolean isRequestIdType(Class<?> paramType) {
        return McpRequestId.class.isAssignableFrom(paramType);
    }

    /**
     * Captured {@link McpPaging} from the most recent invocation (if the tool
     * declared a paging param). Null when no paging was injected. Used by
     * {@link DefaultMcpCallToolResultConverter} to apply automatic slicing on
     * returned lists.
     */
    @Nullable
    public McpPaging capturedPaging() {
        return this.capturedPaging;
    }

    @Nullable
    private McpPaging capturedPaging;

    /**
     * Set the captured paging for this invocation. Called by {@link #buildArgs}
     * after a McpPaging arg is bound; not part of the public extension API.
     */
    public void capturePaging(@Nullable McpPaging paging) {
        this.capturedPaging = paging;
    }

    /**
     * The most recent {@link McpSchema.CallToolRequest} bound to this
     * invocation. Used by {@link DefaultMcpCallToolResultConverter} to
     * forward OpenTelemetry trace context (SEP-414) from request _meta to
     * response _meta.
     */
    @Nullable
    public McpSchema.CallToolRequest currentRequest() {
        return this.currentRequest;
    }

    @Nullable
    private McpSchema.CallToolRequest currentRequest;

    /**
     * Stash the request for the current invocation so the converter can read
     * its {@code _meta}. Called from
     * {@link SyncMcpToolMethodCallback#apply} /
     * {@link AsyncMcpToolMethodCallback#apply}.
     */
    public void captureRequest(@Nullable McpSchema.CallToolRequest request) {
        this.currentRequest = request;
    }

    /** Defensive meta extraction — returns null if request/meta missing or value is wrong type. */
    @Nullable
    private static String extractMetaString(@Nullable McpSchema.CallToolRequest request, String key) {
        if (request == null || request.meta() == null) return null;
        Object v = request.meta().get(key);
        return v instanceof String s ? s : null;
    }

    @Nullable
    private static Integer extractMetaInt(@Nullable McpSchema.CallToolRequest request, String key) {
        if (request == null || request.meta() == null) return null;
        Object v = request.meta().get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    /**
     * 兼容旧调用的 buildArgs 重载，无 request → 不会注入 {@link McpProgress}。
     * 新代码请使用四参重载以启用进度上报。
     */
    protected Object[] buildArgs(Method method, Object exchange, Map<String, Object> arguments) throws JacksonException {
        return buildArgs(method, exchange, arguments, null);
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
        protected McpTool toolAnnotation;
        @Nullable
        protected com.ai.plug.core.spec.dedup.IdempotentCache idempotentCache;
        protected com.ai.plug.core.spec.callback.tool.McpCallToolResultConverter converter;

        protected IRootContext rootContext;
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

        public T rootContext(IRootContext rootContext) {
            this.rootContext = rootContext;
            return (T) this;
        }

        public T converter(McpCallToolResultConverter converter) {
            this.converter = converter;
            return (T) this;
        }

        /**
         * Set the dedup cache for idempotent tools. When set, repeated calls to
         * an {@code @McpTool(idempotentHint=true)} method within the cache's
         * TTL return the cached CallToolResult instead of re-executing.
         */
        public T idempotentCache(@Nullable IdempotentCache cache) {
            // The actual field is on the concrete subclass; subclasses must
            // override this if they want to honor the cache. Default no-op.
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
            this.mineType = tool.mineType();
            this.name = tool.name();
            this.toolAnnotation = tool;
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
