/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.spec.callback.complete;

import com.ai.plug.core.annotation.McpComplete;
import com.ai.plug.core.utils.CompleteAdapter;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CompleteReference;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.DeafaultMcpUriTemplateManagerFactory;
import io.modelcontextprotocol.util.McpUriTemplateManager;
import io.modelcontextprotocol.util.McpUriTemplateManagerFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for creating callbacks around complete methods.
 *
 * This class provides common functionality for both synchronous and asynchronous complete
 * method callbacks. It contains shared logic for method validation, argument building,
 * and other common operations.
 *
 * @author Christian Tzolov
 */
public abstract class AbstractMcpCompleteMethodCallback {

	protected final Method method;

	protected final Object bean;

	protected final String prompt;

	protected final String uri;

	protected final CompleteReference completeReference;

	protected final List<String> uriVariables;

	protected final McpUriTemplateManager uriTemplateManager;


	/**
	 * Constructor for AbstractMcpCompleteMethodCallback.
	 * @param method The method to create a callback for
	 * @param bean The bean instance that contains the method
	 * @param prompt The prompt reference
	 * @param uri The URI reference
	 * @param uriTemplateManagerFactory The URI template manager factory
	 */
	protected AbstractMcpCompleteMethodCallback(Method method, Object bean, String prompt, String uri,
			McpUriTemplateManagerFactory uriTemplateManagerFactory) {

		Assert.notNull(method, "Method can't be null!");
		Assert.notNull(bean, "Bean can't be null!");
		Assert.notNull(uriTemplateManagerFactory, "URI template manager factory can't be null!");

		// Either prompt or uri must be provided, but not both
		if ((prompt == null || prompt.isEmpty()) && (uri == null || uri.isEmpty())) {
			throw new IllegalArgumentException("Either prompt or uri must be provided!");
		}
		if ((prompt != null && !prompt.isEmpty()) && (uri != null && !uri.isEmpty())) {
			throw new IllegalArgumentException("Only one of prompt or uri can be provided!");
		}

		this.method = method;
		this.bean = bean;
		this.prompt = prompt;
		this.uri = uri;

		// Create the CompleteReference based on prompt or uri
		if (prompt != null && !prompt.isEmpty()) {
			this.completeReference = new McpSchema.PromptReference(prompt);
		}
		else {
			this.completeReference = new McpSchema.ResourceReference(uri);
		}

		if (uri != null && !uri.isEmpty()) {
			this.uriTemplateManager = uriTemplateManagerFactory.create(this.uri);
			this.uriVariables = this.uriTemplateManager.getVariableNames();
		}
		else {
			this.uriTemplateManager = null;
			this.uriVariables = new ArrayList<>();
		}

	}

	/**
	 * Validates that the method signature is compatible with the complete callback.
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

		this.validateReturnType(method);
		this.validateParameters(method);
	}

	/**
	 * Validates that the method return type is compatible with the complete callback.
	 * This method should be implemented by subclasses to handle specific return type
	 * validation.
	 * @param method The method to validate
	 * @throws IllegalArgumentException if the return type is not compatible
	 */
	protected void validateReturnType(Method method) {
		// Merge logic for synchronous and asynchronous methods
		// 合并异步和同步的逻辑
		Class<?> returnType = method.getReturnType();

		boolean validReturnType =
				   McpSchema.CompleteResult.class.isAssignableFrom(returnType)
				|| McpSchema.CompleteResult.CompleteCompletion.class.isAssignableFrom(returnType)
				|| List.class.isAssignableFrom(returnType)
				|| String.class.isAssignableFrom(returnType) || Mono.class.isAssignableFrom(returnType)
				|| Mono.class.isAssignableFrom(returnType);


		if (!validReturnType) {
			throw new IllegalArgumentException(
					"Method must return either CompleteResult, CompleteCompletion, List<String>, "
							+ "String, or Mono<T>: " + method.getName() + " in " + method.getDeclaringClass().getName()
							+ " returns " + returnType.getName());
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

		// Check parameter count - must have at most 3 parameters
		if (parameters.length > 3) {
			throw new IllegalArgumentException("Method can have at most 3 input parameters: " + method.getName()
					+ " in " + method.getDeclaringClass().getName() + " has " + parameters.length + " parameters");
		}

		// Check parameter types
		boolean hasExchangeParam = false;
		boolean hasRequestParam = false;
		boolean hasArgumentParam = false;

		for (Parameter param : parameters) {
			Class<?> paramType = param.getType();

			if (isExchangeType(paramType)) {
				if (hasExchangeParam) {
					throw new IllegalArgumentException("Method cannot have more than one exchange parameter: "
							+ method.getName() + " in " + method.getDeclaringClass().getName());
				}
				hasExchangeParam = true;
			}
			else if (CompleteRequest.class.isAssignableFrom(paramType)) {
				if (hasRequestParam) {
					throw new IllegalArgumentException("Method cannot have more than one CompleteRequest parameter: "
							+ method.getName() + " in " + method.getDeclaringClass().getName());
				}
				hasRequestParam = true;
			}
			else if (CompleteRequest.CompleteArgument.class.isAssignableFrom(paramType)) {
				if (hasArgumentParam) {
					throw new IllegalArgumentException("Method cannot have more than one CompleteArgument parameter: "
							+ method.getName() + " in " + method.getDeclaringClass().getName());
				}
				hasArgumentParam = true;
			}
			else if (!String.class.isAssignableFrom(paramType)) {
				throw new IllegalArgumentException(
						"Method parameters must be exchange, CompleteRequest, CompleteArgument, or String: "
								+ method.getName() + " in " + method.getDeclaringClass().getName()
								+ " has parameter of type " + paramType.getName());
			}
		}
	}

	/**
	 * Builds the arguments array for invoking the method.
	 * <p>
	 * This method constructs an array of arguments based on the method's parameter types
	 * and the available values (exchange, request, argument).
	 * @param method The method to build arguments for
	 * @param exchange The server exchange
	 * @param request The complete request
	 * @return An array of arguments for the method invocation
	 */
	protected Object[] buildArgs(Method method, Object exchange, CompleteRequest request) {
		Parameter[] parameters = method.getParameters();
		Object[] args = new Object[parameters.length];

		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			Class<?> paramType = param.getType();

			if (isExchangeType(paramType)) {
				args[i] = exchange;
			}
			else if (CompleteRequest.class.isAssignableFrom(paramType)) {
				args[i] = request;
			}
			else if (CompleteRequest.CompleteArgument.class.isAssignableFrom(paramType)) {
				args[i] = request.argument();
			}
			else if (String.class.isAssignableFrom(paramType)) {
				args[i] = request.argument().value();
			}
			else {
				args[i] = null; // For any other parameter types
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

	/**
	 * Exception thrown when there is an error invoking a complete method.
	 */
	public static class McpCompleteMethodException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructs a new exception with the specified detail message and cause.
		 * @param message The detail message
		 * @param cause The cause
		 */
		public McpCompleteMethodException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * Constructs a new exception with the specified detail message.
		 * @param message The detail message
		 */
		public McpCompleteMethodException(String message) {
			super(message);
		}

	}

	/**
	 * Abstract builder for creating McpCompleteMethodCallback instances.
	 * <p>
	 * This builder provides a base for constructing callback instances with the required
	 * parameters.
	 *
	 * @param <T> The type of the builder
	 * @param <R> The type of the callback
	 */
	protected abstract static class AbstractBuilder<T extends AbstractBuilder<T, R>, R> {

		protected Method method;

		protected Object bean;

		protected McpUriTemplateManagerFactory uriTemplateManagerFactory;

		protected String prompt; // Prompt reference

		protected String uri; // URI reference

		/**
		 * Set the method to create a callback for.
		 * @param method The method to create a callback for
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public T method(Method method) {
			this.method = method;
			return (T) this;
		}

		/**
		 * Set the bean instance that contains the method.
		 * @param bean The bean instance
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public T bean(Object bean) {
			this.bean = bean;
			return (T) this;
		}

		/**
		 * Set the prompt reference.
		 * @param prompt The prompt reference
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public T prompt(String prompt) {
			this.prompt = prompt;
			return (T) this;
		}

		/**
		 * Set the URI reference.
		 * @param uri The URI reference
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public T uri(String uri) {
			this.uri = uri;
			return (T) this;
		}

		/**
		 * Set the complete reference.
		 * @param completeReference The complete reference
		 * @return This builder
		 */
		public T reference(CompleteReference completeReference) {
			if (completeReference instanceof McpSchema.PromptReference promptRef) {
				this.prompt = promptRef.name();
				this.uri = "";
			}
			else if (completeReference instanceof McpSchema.ResourceReference resourceRef) {
				this.prompt = "";
				this.uri = resourceRef.uri();
			}
			return (T) this;
		}

		/**
		 * Set the complete annotation.
		 * @param complete The complete annotation
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public T complete(McpComplete complete) {
			CompleteReference completeRef = CompleteAdapter.asCompleteReference(complete);
			if (completeRef instanceof McpSchema.PromptReference promptRef) {
				this.prompt = promptRef.name();
				this.uri = "";
			}
			else if (completeRef instanceof McpSchema.ResourceReference resourceRef) {
				this.prompt = "";
				this.uri = resourceRef.uri();
			}
			return (T) this;
		}

		/**
		 * Set the URI template manager factory.
		 * @param uriTemplateManagerFactory The URI template manager factory
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public T uriTemplateManagerFactory(McpUriTemplateManagerFactory uriTemplateManagerFactory) {
			this.uriTemplateManagerFactory = uriTemplateManagerFactory;
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
			if ((prompt == null || prompt.isEmpty()) && (uri == null || uri.isEmpty())) {
				throw new IllegalArgumentException("Either prompt or uri must be provided");
			}
			if ((prompt != null && !prompt.isEmpty()) && (uri != null && !uri.isEmpty())) {
				throw new IllegalArgumentException("Only one of prompt or uri can be provided");
			}
			if (this.uriTemplateManagerFactory == null) {
				this.uriTemplateManagerFactory = new DeafaultMcpUriTemplateManagerFactory();
			}
		}

		/**
		 * Build the callback.
		 * @return A new callback instance
		 */
		public abstract R build();

	}

}
