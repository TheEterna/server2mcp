/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.spec.callback.resource;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.DeafaultMcpUriTemplateManagerFactory;
import io.modelcontextprotocol.util.McpUriTemplateManager;
import io.modelcontextprotocol.util.McpUriTemplateManagerFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 资源方法回调的抽象基类。
 * 该类为同步和异步资源方法回调提供了公共功能。
 * 包含方法验证、参数构建等通用操作的实现。
 *
 * @author Christian Tzolov
 */
public abstract class AbstractMcpResourceMethodCallback {

	/**
	 * Content type of the resource.
	 */
	public enum ContentType {

		/**
		 * Text content type.
		 */
		TEXT,

		/**
		 * Binary blob content type.
		 */
		BLOB

	}

	protected final Method method;

	protected final Object bean;

	protected final String uri;

	protected final String name;
	protected final String title;

	protected final String description;

	protected final String mimeType;

	protected final List<String> uriVariables;

	protected final McpReadResourceResultConverter resultConverter;

	protected final McpUriTemplateManager uriTemplateManager;

	protected final ContentType contentType;

	/**
	 * Constructor for AbstractMcpResourceMethodCallback.
	 * @param method The method to create a callback for
	 * @param bean The bean instance that contains the method
	 * @param resourceAnnotation The resource annotation
	 * @param resultConverter The result converter
	 * @param uriTemplateMangerFactory The URI template manager factory
	 * @param contentType The content type
	 */
	protected AbstractMcpResourceMethodCallback(Method method, Object bean, String uri, String name, String title, String description,
			String mimeType, McpReadResourceResultConverter resultConverter,
			McpUriTemplateManagerFactory uriTemplateMangerFactory, ContentType contentType) {

		Assert.hasText(uri, "URI can't be null or empty!");
		Assert.notNull(method, "Method can't be null!");
		Assert.notNull(bean, "Bean can't be null!");
		Assert.notNull(resultConverter, "Result converter can't be null!");
		Assert.notNull(uriTemplateMangerFactory, "URI template manager factory can't be null!");

		this.method = method;
		this.bean = bean;
		this.uri = uri;
		this.name = name;
		this.title = title;
		this.description = description;
		this.mimeType = mimeType;
		this.resultConverter = resultConverter;
		this.uriTemplateManager = uriTemplateMangerFactory.create(this.uri);

		this.uriVariables = this.uriTemplateManager.getVariableNames();

		this.contentType = contentType;
	}

	/**
	 * Validates that the method signature is compatible with the resource callback.
	 * <p>
	 * This method checks that the return type is valid and that the parameters match the
	 * expected pattern based on whether URI variables are present.
	 * @param method The method to validate
	 * @throws IllegalArgumentException if the method signature is not compatible
	 */
	protected void validateMethod(Method method) {
		if (method == null) {
			throw new IllegalArgumentException("Method must not be null");
		}

		this.validateReturnType(method);

		if (this.uriVariables.isEmpty()) {
			this.validateParametersWithoutUriVariables(method);
		}
		else {
			this.validateParametersWithUriVariables(method);
		}
	}

	/**
	 * Validates that the method return type is compatible with the resource callback.
	 * This method should be implemented by subclasses to handle specific return type
	 * validation.
	 * @param method The method to validate
	 * @throws IllegalArgumentException if the return type is not compatible
	 */
	protected void validateReturnType(Method method) {
		Class<?> returnType = method.getReturnType();

		boolean validReturnType = McpSchema.ReadResourceResult.class.isAssignableFrom(returnType)
				|| List.class.isAssignableFrom(returnType) || McpSchema.ResourceContents.class.isAssignableFrom(returnType)
				|| String.class.isAssignableFrom(returnType) || Mono.class.isAssignableFrom(returnType);

		if (!validReturnType) {
			throw new IllegalArgumentException(
					"Method must return either ReadResourceResult, List<ResourceContents>, List<String>, "
							+ "ResourceContents, String, or Mono<T>: " + method.getName() + " in "
							+ method.getDeclaringClass().getName() + " returns " + returnType.getName());
		}
	}
	/**
	 * Validates method parameters when no URI variables are present. This method provides
	 * common validation logic and delegates exchange type checking to subclasses.
	 * @param method The method to validate
	 * @throws IllegalArgumentException if the parameters are not compatible
	 */
	protected void validateParametersWithoutUriVariables(Method method) {
		Parameter[] parameters = method.getParameters();

		// Check parameter count - must have at most 2 parameters
		if (parameters.length > 2) {
			throw new IllegalArgumentException(
					"Method can have at most 2 input parameters when no URI variables are present: " + method.getName()
							+ " in " + method.getDeclaringClass().getName() + " has " + parameters.length
							+ " parameters");
		}

		// Check parameter types
		boolean hasValidParams = false;
		boolean hasExchangeParam = false;
		boolean hasRequestOrUriParam = false;

		for (Parameter param : parameters) {
			Class<?> paramType = param.getType();

			if (isExchangeType(paramType)) {
				if (hasExchangeParam) {
					throw new IllegalArgumentException("Method cannot have more than one exchange parameter: "
							+ method.getName() + " in " + method.getDeclaringClass().getName());
				}
				hasExchangeParam = true;
			}
			else if (ReadResourceRequest.class.isAssignableFrom(paramType)
					|| String.class.isAssignableFrom(paramType)) {
				if (hasRequestOrUriParam) {
					throw new IllegalArgumentException(
							"Method cannot have more than one ReadResourceRequest or String parameter: "
									+ method.getName() + " in " + method.getDeclaringClass().getName());
				}
				hasRequestOrUriParam = true;
				hasValidParams = true;
			}
			else {
				throw new IllegalArgumentException(
						"Method parameters must be exchange, ReadResourceRequest, or String when no URI variables are present: "
								+ method.getName() + " in " + method.getDeclaringClass().getName()
								+ " has parameter of type " + paramType.getName());
			}
		}

		if (!hasValidParams && parameters.length > 0) {
			throw new IllegalArgumentException(
					"Method must have either ReadResourceRequest or String parameter when no URI variables are present: "
							+ method.getName() + " in " + method.getDeclaringClass().getName());
		}
	}

	/**
	 * Validates method parameters when URI variables are present. This method provides
	 * common validation logic and delegates exchange type checking to subclasses.
	 * @param method The method to validate
	 * @throws IllegalArgumentException if the parameters are not compatible
	 */
	protected void validateParametersWithUriVariables(Method method) {
		Parameter[] parameters = method.getParameters();

		// Count special parameters (exchange and request)
		int exchangeParamCount = 0;
		int requestParamCount = 0;

		for (Parameter param : parameters) {
			Class<?> paramType = param.getType();
			if (isExchangeType(paramType)) {
				exchangeParamCount++;
			}
			else if (ReadResourceRequest.class.isAssignableFrom(paramType)) {
				requestParamCount++;
			}
		}

		// Check if we have more than one exchange parameter
		if (exchangeParamCount > 1) {
			throw new IllegalArgumentException("Method cannot have more than one exchange parameter: "
					+ method.getName() + " in " + method.getDeclaringClass().getName());
		}

		// Check if we have more than one request parameter
		if (requestParamCount > 1) {
			throw new IllegalArgumentException("Method cannot have more than one ReadResourceRequest parameter: "
					+ method.getName() + " in " + method.getDeclaringClass().getName());
		}

		// Calculate how many parameters should be for URI variables
		int specialParamCount = exchangeParamCount + requestParamCount;
		int uriVarParamCount = parameters.length - specialParamCount;

		// Check if we have the right number of parameters for URI variables
		if (uriVarParamCount != this.uriVariables.size()) {
			throw new IllegalArgumentException(
					"Method must have parameters for all URI variables. Expected " + this.uriVariables.size()
							+ " URI variable parameters, but found " + uriVarParamCount + ": " + method.getName()
							+ " in " + method.getDeclaringClass().getName() + ". URI variables: " + this.uriVariables);
		}

		// Check that all non-special parameters are String type (for URI variables)
		for (Parameter param : parameters) {
			Class<?> paramType = param.getType();
			if (!isExchangeType(paramType) && !ReadResourceRequest.class.isAssignableFrom(paramType)
					&& !String.class.isAssignableFrom(paramType)) {
				throw new IllegalArgumentException("URI variable parameters must be of type String: " + method.getName()
						+ " in " + method.getDeclaringClass().getName() + ", parameter of type " + paramType.getName()
						+ " is not valid");
			}
		}
	}

	/**
	 * Builds the arguments array for invoking the method.
	 * <p>
	 * This method constructs an array of arguments based on the method's parameter types
	 * and the available values (exchange, request, URI variables).
	 * @param method The method to build arguments for
	 * @param exchange The server exchange
	 * @param request The resource request
	 * @param uriVariableValues Map of URI variable names to their values
	 * @return An array of arguments for the method invocation
	 */
	protected Object[] buildArgs(Method method, Object exchange, ReadResourceRequest request,
			Map<String, String> uriVariableValues) {
		Parameter[] parameters = method.getParameters();
		Object[] args = new Object[parameters.length];

		if (!this.uriVariables.isEmpty()) {
			this.buildArgsWithUriVariables(parameters, args, exchange, request, uriVariableValues);
		}
		else {
			this.buildArgsWithoutUriVariables(parameters, args, exchange, request);
		}

		return args;
	}

	/**
	 * 为包含URI变量的方法构建参数。该方法为包含URI变量的方法提供通用的参数构建逻辑。
	 * @param parameters 方法参数
	 * @param args 需要填充的参数数组
	 * @param exchange 服务器交换对象
	 * @param request 资源请求对象
	 * @param uriVariableValues URI变量名称到值的映射
	 */
	protected void buildArgsWithUriVariables(Parameter[] parameters, Object[] args, Object exchange,
			ReadResourceRequest request, Map<String, String> uriVariableValues) {

		// Track which URI variables have been assigned
		List<String> assignedVariables = new ArrayList<>();

		// First pass: assign special parameters (exchange and request)
		for (int i = 0; i < parameters.length; i++) {
			Class<?> paramType = parameters[i].getType();
			if (isExchangeType(paramType)) {
				args[i] = exchange;
			}
			else if (ReadResourceRequest.class.isAssignableFrom(paramType)) {
				args[i] = request;
			}
		}

		// 2. 将URI变量分配给剩余的参数
		int variableIndex = 0;
		for (int i = 0; i < parameters.length; i++) {
			// Skip parameters that already have values (exchange or request)
			if (args[i] != null) {
				continue;
			}

			// Assign the next URI variable
			if (variableIndex < this.uriVariables.size()) {
				String variableName = this.uriVariables.get(variableIndex);
				args[i] = uriVariableValues.get(variableName);
				assignedVariables.add(variableName);
				variableIndex++;
			}
		}

		// Verify all URI variables were assigned
		if (assignedVariables.size() != this.uriVariables.size()) {
			throw new IllegalArgumentException("Failed to assign all URI variables to method parameters. "
					+ "Assigned: " + assignedVariables + ", Expected: " + this.uriVariables);
		}
	}

	/**
	 * 为不包含URI变量的方法构建参数。该方法为不包含URI变量的方法提供通用的参数构建逻辑。
	 * @param parameters 方法参数
	 * @param args 需要填充的参数数组
	 * @param exchange 服务器交换对象
	 * @param request 资源请求对象
	 */
	protected void buildArgsWithoutUriVariables(Parameter[] parameters, Object[] args, Object exchange,
			ReadResourceRequest request) {
		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			Class<?> paramType = param.getType();

			if (isExchangeType(paramType)) {
				args[i] = exchange;
			}
			else if (ReadResourceRequest.class.isAssignableFrom(paramType)) {
				args[i] = request;
			}
			else if (String.class.isAssignableFrom(paramType)) {
				args[i] = request.uri();
			}
			else {
				args[i] = null; // For any other parameter types
			}
		}
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
	 * Returns the content type of the resource.
	 * @return the content type
	 */
	public ContentType contentType() {
		return this.contentType;
	}

	/**
	 * Exception thrown when there is an error invoking a resource method.
	 */
	public static class McpResourceMethodException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructs a new exception with the specified detail message and cause.
		 * @param message The detail message
		 * @param cause The cause
		 */
		public McpResourceMethodException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * Constructs a new exception with the specified detail message.
		 * @param message The detail message
		 */
		public McpResourceMethodException(String message) {
			super(message);
		}

	}

	/**
	 * Abstract builder for creating McpResourceMethodCallback instances.
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

		protected McpReadResourceResultConverter resultConverter;

		protected McpUriTemplateManagerFactory uriTemplateManagerFactory;

		protected ContentType contentType;

		protected String name;

		protected String title; // Optional title for the resource

		protected String description; // Optional description for the resource

		protected String mimeType; // Optional MIME type for the resource

		protected String uri; // Resource URI

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
		 * Set the URI template manager factory.
		 * @param uri The URI template manager factory
		 * @return This builder
		 */
		public T uri(String uri) {
			this.uri = uri;
			return (T) this;
		}

		/**
		 * Set the Mcp Schema resource.
		 * @param resource The resource
		 * @return This builder
		 */
		public T resource(McpSchema.Resource resource) {
			this.uri = resource.uri();
			this.name = resource.name();
			this.description = resource.description();
			this.mimeType = resource.mimeType();
			return (T) this;
		}

		/**
		 * Set the Mcp Schema resource template.
		 * @param resourceTemplate The resource template
		 * @return This builder
		 */
		public T resource(McpSchema.ResourceTemplate resourceTemplate) {
			this.uri = resourceTemplate.uriTemplate();
			this.name = resourceTemplate.name();
			this.description = resourceTemplate.description();
			this.mimeType = resourceTemplate.mimeType();
			return (T) this;
		}

		/**
		 * Set the result converter.
		 * @param resultConverter The result converter
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public T resultConverter(McpReadResourceResultConverter resultConverter) {
			this.resultConverter = resultConverter;
			return (T) this;
		}

		/**
		 * Set the URI template.
		 * @param uriTemplateManager The URI template
		 * @return This builder
		 */
		@SuppressWarnings("unchecked")
		public T uriTemplateManagerFactory(McpUriTemplateManagerFactory uriTemplateManagerFactory) {
			this.uriTemplateManagerFactory = uriTemplateManagerFactory;
			return (T) this;
		}

		/**
		 * Set the content type.
		 * @param contentType The content type
		 * @return This builder
		 */
		public T contentType(ContentType contentType) {
			this.contentType = contentType;
			return (T) this;
		}

		/**
		 * Set the name of the resource.
		 * @param name The name of the resource
		 * @return This builder
		 */
		public T name(String name) {
			this.name = name;
			return (T) this;
		}

		/**
		 * Set the description of the resource.
		 * @param description The description of the resource
		 * @return This builder
		 */
		public T description(String description) {
			this.description = description;
			return (T) this;
		}

		/**
		 * Set the MIME type of the resource.
		 * @param mimeType The MIME type of the resource
		 * @return This builder
		 */
		public T mimeType(String mimeType) {
			this.mimeType = mimeType;
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
			if (this.uri == null || this.uri.isEmpty()) {
				throw new IllegalArgumentException("URI must not be null or empty");
			}
			if (this.uriTemplateManagerFactory == null) {
				this.uriTemplateManagerFactory = new DeafaultMcpUriTemplateManagerFactory();
			}
			if (this.mimeType == null) {
				this.mimeType = "text/plain";
			}

			if (this.name == null) {
				this.name = method.getName();
			}
		}

		/**
		 * Build the callback.
		 * @return A new callback instance
		 */
		public abstract R build();

	}

}
