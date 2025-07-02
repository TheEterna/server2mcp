/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.spec.callback.prompt;

import com.logaritex.mcp.annotation.McpPrompt;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

/**
 * Class for creating BiFunction callbacks around prompt methods with asynchronous
 * processing.
 *
 * This class provides a way to convert methods annotated with {@link McpPrompt} into
 * callback functions that can be used to handle prompt requests asynchronously. It
 * supports various method signatures and return types.
 *
 * @author Christian Tzolov
 */
public final class AsyncMcpPromptMethodCallback extends AbstractMcpPromptMethodCallback
		implements BiFunction<McpAsyncServerExchange, GetPromptRequest, Mono<GetPromptResult>> {

	private AsyncMcpPromptMethodCallback(Builder builder) {
		super(builder.method, builder.bean, builder.prompt);
	}

	/**
	 * Apply the callback to the given exchange and request.
	 * <p>
	 * This method builds the arguments for the method call, invokes the method, and
	 * converts the result to a GetPromptResult.
	 * @param exchange The server exchange, may be null if the method doesn't require it
	 * @param request The prompt request, must not be null
	 * @return A Mono that emits the prompt result
	 * @throws McpPromptMethodException if there is an error invoking the prompt method
	 * @throws IllegalArgumentException if the request is null
	 */
	@Override
	public Mono<GetPromptResult> apply(McpAsyncServerExchange exchange, GetPromptRequest request) {
		if (request == null) {
			return Mono.error(new IllegalArgumentException("Request must not be null"));
		}

		return Mono.defer(() -> {
			try {
				// Build arguments for the method call
				Object[] args = this.buildArgs(this.method, exchange, request);

				// Invoke the method
				this.method.setAccessible(true);
				Object result = this.method.invoke(this.bean, args);

				// Handle the result based on its type
				if (result instanceof Mono<?>) {
					// If the result is already a Mono, map it to a GetPromptResult
					return ((Mono<?>) result).map((r) -> convertToGetPromptResult(r));
				}
				else {
					// Otherwise, convert the result to a GetPromptResult and wrap in a
					// Mono
					return Mono.just(convertToGetPromptResult(result));
				}
			}
			catch (Exception e) {
				return Mono
						.error(new McpPromptMethodException("Error invoking prompt method: " + this.method.getName(), e));
			}
		});
	}
	/**
	 * Validates that the method return type is compatible with the prompt callback.
	 * @param paramType The paramType to validate
	 * @throws IllegalArgumentException if the return type is not compatible
	 */
	@Override
	protected boolean isExchangeType(Class<?> paramType) {
		return McpAsyncServerExchange.class.isAssignableFrom(paramType);
	}



	/**
	 * Builder for creating AsyncMcpPromptMethodCallback instances.
	 * <p>
	 * This builder provides a fluent API for constructing AsyncMcpPromptMethodCallback
	 * instances with the required parameters.
	 */
	public static class Builder extends AbstractBuilder<Builder, AsyncMcpPromptMethodCallback> {

		/**
		 * Build the callback.
		 * @return A new AsyncMcpPromptMethodCallback instance
		 */
		@Override
		public AsyncMcpPromptMethodCallback build() {
			validate();
			return new AsyncMcpPromptMethodCallback(this);
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
