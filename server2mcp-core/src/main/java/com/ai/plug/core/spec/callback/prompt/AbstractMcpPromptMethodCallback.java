/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.spec.callback.prompt;

import com.ai.plug.common.utils.JsonParser;
import com.ai.plug.core.annotation.McpArg;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.util.Assert;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for creating callbacks around prompt methods.
 *
 * This class provides common functionality for both synchronous and asynchronous prompt
 * method callbacks.
 *
 * @author Christian Tzolov
 */
public abstract class AbstractMcpPromptMethodCallback {

	protected final Method method;

	protected final Object bean;

	protected final Prompt prompt;

	/**
	 * Constructor for AbstractMcpPromptMethodCallback.
	 * @param method The method to create a callback for
	 * @param bean The bean instance that contains the method
	 * @param prompt The prompt
	 */
	protected AbstractMcpPromptMethodCallback(Method method, Object bean, Prompt prompt) {
		this.method = method;
		this.bean = bean;
		this.prompt = prompt;
		this.validateMethod(this.method);
	}

	/**
	 * Validates that the method signature is compatible with the prompt callback.
	 * @param method The method to validate
	 * @throws IllegalArgumentException if the method signature is not compatible
	 */
	protected void validateMethod(Method method) {
		if (method == null) {
			throw new IllegalArgumentException("Method must not be null");
		}

		this.validateReturnType(method);
		this.validateParameters(method);
	}

	/**
	 * Validates that the method return type is compatible with the prompt callback.
	 * @param method The method to validate
	 * @throws IllegalArgumentException if the return type is not compatible
	 */
	protected void validateReturnType(Method method) {
		// Merge logic for synchronous and asynchronous methods
		// 合并异步和同步的逻辑
		Class<?> returnType = method.getReturnType();

		boolean validReturnType = GetPromptResult.class.isAssignableFrom(returnType)
				|| List.class.isAssignableFrom(returnType)
				|| PromptMessage.class.isAssignableFrom(returnType)
				|| String.class.isAssignableFrom(returnType)
				|| Mono.class.isAssignableFrom(returnType);

		if (!validReturnType) {
			throw new IllegalArgumentException(
					"Method must return either GetPromptResult, List<PromptMessage>, PromptMessage, List<String>, "
							+ "String, or Mono<T>: " + method.getName() + " in " + method.getDeclaringClass().getName()
							+ " returns " + returnType.getName());
		}
	}
	/**
	 * Checks if a parameter type is compatible with the exchange type.
	 * @param paramType The parameter type to check
	 * @return true if the parameter type is compatible with the exchange type, false
	 * otherwise
	 */
	protected abstract boolean isExchangeType(Class<?> paramType);

	/**
	 * Validates method parameters.
	 * @param method The method to validate
	 * @throws IllegalArgumentException if the parameters are not compatible
	 */
	protected void validateParameters(Method method) {
		java.lang.reflect.Parameter[] parameters = method.getParameters();

		// Check for duplicate parameter types
		boolean hasExchangeParam = false;
		boolean hasRequestParam = false;
		boolean hasMapParam = false;

		for (java.lang.reflect.Parameter param : parameters) {
			Class<?> paramType = param.getType();

			if (isExchangeType(paramType)) {
				if (hasExchangeParam) {
					throw new IllegalArgumentException("Method cannot have more than one exchange parameter: "
							+ method.getName() + " in " + method.getDeclaringClass().getName());
				}
				hasExchangeParam = true;
			}
			else if (GetPromptRequest.class.isAssignableFrom(paramType)) {
				if (hasRequestParam) {
					throw new IllegalArgumentException("Method cannot have more than one GetPromptRequest parameter: "
							+ method.getName() + " in " + method.getDeclaringClass().getName());
				}
				hasRequestParam = true;
			}
			else if (Map.class.isAssignableFrom(paramType)) {
				if (hasMapParam) {
					throw new IllegalArgumentException("Method cannot have more than one Map parameter: "
							+ method.getName() + " in " + method.getDeclaringClass().getName());
				}
				hasMapParam = true;
			}
			// Other parameter types are assumed to be individual arguments
		}
	}

	/**
	 * Builds the arguments array for invoking the method.
	 * <p>
	 * This method constructs an array of arguments based on the method's parameter types
	 * and the available values (exchange, request, arguments).
	 * @param method The method to build arguments for
	 * @param exchange The server exchange
	 * @param request The prompt request
	 * @return An array of arguments for the method invocation
	 */
	@SneakyThrows
	public Object[] buildArgs(Method method, Object exchange, GetPromptRequest request) {
		java.lang.reflect.Parameter[] parameters = method.getParameters();
		Object[] args = new Object[parameters.length];

		for (int i = 0; i < parameters.length; i++) {
			java.lang.reflect.Parameter param = parameters[i];
			Class<?> paramType = param.getType();

			if (isExchangeType(paramType)) {
				args[i] = exchange;
			}
			else if (GetPromptRequest.class.isAssignableFrom(paramType)) {
				args[i] = request;
			}
			else if (Map.class.isAssignableFrom(paramType)) {
				args[i] = request.arguments() != null ? request.arguments() : new HashMap<>();
			}
			else {
				// For individual argument parameters, extract from the request arguments
				McpArg arg = param.getAnnotation(McpArg.class);
				String paramName = arg != null && !arg.name().isBlank() ? arg.name() : param.getName();

				if (request.arguments() != null && request.arguments().containsKey(paramName)) {
					Object argValue = request.arguments().get(paramName);
					args[i] = JsonParser.toTypedObject(argValue, paramType);

				}
				else {
					args[i] = null; // No matching argument found
				}
			}
		}

		return args;
	}

	/**
	 * Converts a method result to a GetPromptResult.
	 * @param result The result to convert
	 * @return The converted GetPromptResult
	 */
	@SuppressWarnings("unchecked")
	protected GetPromptResult convertToGetPromptResult(Object result) {
		if (result instanceof GetPromptResult) {
			return (GetPromptResult) result;
		}
		else if (result instanceof List) {
			List<?> list = (List<?>) result;
			if (!list.isEmpty()) {
				if (list.get(0) instanceof PromptMessage) {
					return new GetPromptResult(null, (List<PromptMessage>) list);
				}
				else if (list.get(0) instanceof String) {
					// Convert List<String> to List<PromptMessage>
					List<PromptMessage> messages = ((List<String>) list).stream()
						.map(text -> new PromptMessage(McpSchema.Role.ASSISTANT,
								new McpSchema.TextContent(text)))
						.collect(java.util.stream.Collectors.toList());
					return new GetPromptResult(null, messages);
				}
			}
		}
		else if (result instanceof PromptMessage) {
			// If the result is a single PromptMessage, wrap it in a list
			return new GetPromptResult(null, List.of((PromptMessage) result));
		}
		else if (result instanceof String) {
			// If the result is a simple string, create a single assistant message with
			// that content
			return new GetPromptResult(null,
					List.of(new PromptMessage(McpSchema.Role.ASSISTANT,
							new McpSchema.TextContent((String) result))));
		}

		throw new IllegalArgumentException(
				"Unsupported result type: " + (result != null ? result.getClass().getName() : "null"));
	}

	/**
	 * Abstract builder for creating prompt method callback instances.
	 *
	 * @param <B> The builder type
	 * @param <T> The callback type
	 */
	protected abstract static class AbstractBuilder<B extends AbstractBuilder<B, T>, T extends AbstractMcpPromptMethodCallback> {

		protected Method method;

		protected Object bean;

		protected Prompt prompt;

		/**
		 * Set the method to create a callback for.
		 * @param method The method to create a callback for
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public B method(Method method) {
			this.method = method;
			return (B) this;
		}

		/**
		 * Set the bean instance that contains the method.
		 * @param bean The bean instance
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public B bean(Object bean) {
			this.bean = bean;
			return (B) this;
		}

		/**
		 * Set the prompt.
		 * @param prompt The prompt
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public B prompt(Prompt prompt) {
			this.prompt = prompt;
			return (B) this;
		}

		/**
		 * Validate the builder state.
		 * @throws IllegalArgumentException if the builder state is invalid
		 */
		protected void validate() {
			Assert.notNull(method, "Method must not be null");
			Assert.notNull(bean, "Bean must not be null");
			Assert.notNull(prompt, "Prompt must not be null");
		}

		/**
		 * Build the callback.
		 * @return A new callback instance
		 */
		public abstract T build();

	}

	/**
	 * Exception thrown when there is an error invoking a prompt method.
	 */
	public static class McpPromptMethodException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructs a new exception with the specified detail message and cause.
		 * @param message The detail message
		 * @param cause The cause
		 */
		public McpPromptMethodException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * Constructs a new exception with the specified detail message.
		 * @param message The detail message
		 */
		public McpPromptMethodException(String message) {
			super(message);
		}

	}

}
