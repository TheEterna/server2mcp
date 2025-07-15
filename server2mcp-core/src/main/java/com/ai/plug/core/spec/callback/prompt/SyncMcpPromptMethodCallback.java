/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.spec.callback.prompt;

import com.ai.plug.core.annotation.McpPrompt;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

/**
 * 用于创建围绕提示方法的BiFunction回调的类。
 *
 * 该类提供了一种将使用{@link McpPrompt}注解的方法转换为可用于处理提示请求的回调函数的方式。
 * 它支持各种方法签名和返回类型。
 *
 * @author Christian Tzolov
 */
public final class SyncMcpPromptMethodCallback extends com.ai.plug.core.spec.callback.prompt.AbstractMcpPromptMethodCallback
		implements BiFunction<McpSyncServerExchange, GetPromptRequest, GetPromptResult> {

	Logger logger = LoggerFactory.getLogger(SyncMcpPromptMethodCallback.class);

	private SyncMcpPromptMethodCallback(Builder builder) {
		super(builder.method, builder.bean, builder.prompt);
	}

	/**
	 * Apply the callback to the given exchange and request.
	 * <p>
	 * This method builds the arguments for the method call, invokes the method, and
	 * converts the result to a GetPromptResult.
	 * @param exchange The server exchange, may be null if the method doesn't require it
	 * @param request The prompt request, must not be null
	 * @return The prompt result
	 * @throws McpPromptMethodException if there is an error invoking the prompt method
	 * @throws IllegalArgumentException if the request is null
	 */
	@Override
	public GetPromptResult apply(McpSyncServerExchange exchange, GetPromptRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Request must not be null");
		}

		try {
			// Build arguments for the method call
			Object[] args = this.buildArgs(this.method, exchange, request);

			// Invoke the method
			this.method.setAccessible(true);
			Object result = this.method.invoke(this.bean, args);
			if (result instanceof Mono<?>) {
				// If the result is already a Mono, map it to a GetPromptResult
				result = ((Mono<?>) result).block();
			}
			// Convert the result to a GetPromptResult
			return this.convertToGetPromptResult(result);

		}
		catch (Exception e) {
			throw new McpPromptMethodException("Error invoking prompt method: " + this.method.getName(), e);
		}
	}



	/**
	 * Validates that the method return type is compatible with the prompt callback.
	 * @param paramType The paramType to validate
	 * @throws IllegalArgumentException if the return type is not compatible
	 */
	@Override
	protected boolean isExchangeType(Class<?> paramType) {
		return McpSyncServerExchange.class.isAssignableFrom(paramType);
	}


	/**
	 * Builder for creating SyncMcpPromptMethodCallback instances.
	 * <p>
	 * This builder provides a fluent API for constructing SyncMcpPromptMethodCallback
	 * instances with the required parameters.
	 */
	public static class Builder extends AbstractBuilder<Builder, SyncMcpPromptMethodCallback> {

		/**
		 * Build the callback.
		 * @return A new SyncMcpPromptMethodCallback instance
		 */
		@Override
		public SyncMcpPromptMethodCallback build() {
			validate();
			return new SyncMcpPromptMethodCallback(this);
		}

	}

	/**
	 * Create a new builder.
	 * @return A new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

}
