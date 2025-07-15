/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.spec.callback.resource;

import com.ai.plug.core.annotation.McpResource;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Class for creating BiFunction callbacks around resource methods.
 *
 * This class provides a way to convert methods annotated with {@link McpResource} into
 * callback functions that can be used to handle resource requests. It supports various
 * method signatures and return types, and handles URI template variables.
 *
 * @author Christian Tzolov
 */
public final class SyncMcpResourceMethodCallback extends AbstractMcpResourceMethodCallback
		implements BiFunction<McpSyncServerExchange, ReadResourceRequest, ReadResourceResult> {

	private SyncMcpResourceMethodCallback(Builder builder) {
		super(builder.method, builder.bean, builder.uri, builder.name, builder.title, builder.description, builder.mimeType,
				builder.resultConverter, builder.uriTemplateManagerFactory, builder.contentType);
		this.validateMethod(this.method);
	}

	/**
	 * 将回调应用于给定的交换和请求。
	 * <p>
	 * 此方法从请求URI中提取URI变量值，构建方法调用的参数，调用方法，并将结果转换为
	 * ReadResourceResult。
	 * @param exchange 服务器交换，如果方法不需要它可以为null
	 * @param request 资源请求，必须不为null
	 * @return 资源结果
	 * @throws McpResourceMethodException 如果调用资源方法时发生错误
	 * @throws IllegalArgumentException 如果请求为null或URI变量提取失败
	 */
	@Override
	public ReadResourceResult apply(McpSyncServerExchange exchange, ReadResourceRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Request must not be null");
		}

		try {
			// Extract URI variable values from the request URI
			Map<String, String> uriVariableValues = this.uriTemplateManager.extractVariableValues(request.uri());

			// 如果需要URI变量，请验证是否提取了所有URI变量

			if (!this.uriVariables.isEmpty() && uriVariableValues.size() != this.uriVariables.size()) {
				throw new IllegalArgumentException("Failed to extract all URI variables from request URI: "
						+ request.uri() + ". Expected variables: " + this.uriVariables + ", but found: "
						+ uriVariableValues.keySet());
			}

			// Build arguments for the method call
			Object[] args = this.buildArgs(this.method, exchange, request, uriVariableValues);

			// invoke the method
			this.method.setAccessible(true);
			Object result = this.method.invoke(this.bean, args);

			// convert Mono
			if (result instanceof Mono<?>) {
				// If the result is already a Mono, map it to a GetPromptResult
				result = ((Mono<?>) result).block();
			}

			// Convert the result to a ReadResourceResult using the 转换器
			return this.resultConverter.convertToReadResourceResult(result, request.uri(), this.mimeType,
					this.contentType);
		}
		catch (Exception e) {
			throw new McpResourceMethodException("Access error invoking resource method: " + this.method.getName(), e);
		}
	}

	/**
	 * Builder for creating SyncMcpResourceMethodCallback instances.
	 * <p>
	 * This builder provides a fluent API for constructing SyncMcpResourceMethodCallback
	 * instances with the required parameters.
	 */
	public static class Builder extends AbstractBuilder<Builder, SyncMcpResourceMethodCallback> {

		/**
		 * Constructor for Builder.
		 */
		public Builder() {
			this.resultConverter = new DefaultMcpReadResourceResultConverter();
		}

		/**
		 * Build the callback.
		 * @return A new SyncMcpResourceMethodCallback instance
		 */
		@Override
		public SyncMcpResourceMethodCallback build() {
			validate();
			return new SyncMcpResourceMethodCallback(this);
		}

	}

	/**
	 * Create a new builder.
	 * @return A new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}


	/**
	 * Checks if a parameter type is compatible with the exchange type.
	 * @param paramType The parameter type to check
	 * @return true if the parameter type is compatible with the exchange type, false
	 * otherwise
	 */
	@Override
	protected boolean isExchangeType(Class<?> paramType) {
		return McpSyncServerExchange.class.isAssignableFrom(paramType);
	}

}
