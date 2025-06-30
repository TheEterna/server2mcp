/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ai.plug.core.provider;

import com.logaritex.mcp.annotation.McpLoggingConsumer;
import com.logaritex.mcp.method.logging.AsyncMcpLoggingConsumerMethodCallback;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.util.Assert;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Provider for asynchronous logging consumer callbacks.
 *
 * <p>
 * This class scans a list of objects for methods annotated with
 * {@link McpLoggingConsumer} and creates {@link Function} callbacks for them. These
 * callbacks can be used to handle logging message notifications from MCP servers in a
 * reactive way.
 *
 * <p>
 * Example usage: <pre>{@code
 * // Create a provider with a list of objects containing @McpLoggingConsumer methods
 * AsyncMcpLoggingConsumerProvider provider = new AsyncMcpLoggingConsumerProvider(List.of(loggingHandler));
 *
 * // Get the list of logging consumer callbacks
 * List<Function<LoggingMessageNotification, Mono<Void>>> consumers = provider.getLoggingConsumers();
 *
 * // Add the consumers to the client features
 * McpClientFeatures.Async clientFeatures = new McpClientFeatures.Async(
 *     clientInfo, clientCapabilities, roots,
 *     toolsChangeConsumers, resourcesChangeConsumers, promptsChangeConsumers,
 *     consumers, samplingHandler);
 * }</pre>
 *
 * @author Christian Tzolov
 * @see McpLoggingConsumer
 * @see AsyncMcpLoggingConsumerMethodCallback
 * @see LoggingMessageNotification
 */
public class AsyncMcpLoggingConsumerProvider {

	private final List<Object> loggingConsumerObjects;

	/**
	 * Create a new AsyncMcpLoggingConsumerProvider.
	 * @param loggingConsumerObjects the objects containing methods annotated with
	 * {@link McpLoggingConsumer}
	 */
	public AsyncMcpLoggingConsumerProvider(List<Object> loggingConsumerObjects) {
		Assert.notNull(loggingConsumerObjects, "loggingConsumerObjects cannot be null");
		this.loggingConsumerObjects = loggingConsumerObjects;
	}

	/**
	 * Get the list of logging consumer callbacks.
	 * @return the list of logging consumer callbacks
	 */
	public List<Function<LoggingMessageNotification, Mono<Void>>> getLoggingConsumers() {

		List<Function<LoggingMessageNotification, Mono<Void>>> loggingConsumers = this.loggingConsumerObjects.stream()
			.map(consumerObject -> Stream.of(doGetClassMethods(consumerObject))
				.filter(method -> method.isAnnotationPresent(McpLoggingConsumer.class))
				.map(mcpLoggingConsumerMethod -> {
					var loggingConsumerAnnotation = mcpLoggingConsumerMethod.getAnnotation(McpLoggingConsumer.class);

					Function<LoggingMessageNotification, Mono<Void>> methodCallback = AsyncMcpLoggingConsumerMethodCallback
						.builder()
						.method(mcpLoggingConsumerMethod)
						.bean(consumerObject)
						.loggingConsumer(loggingConsumerAnnotation)
						.build();

					return methodCallback;
				})
				.toList())
			.flatMap(List::stream)
			.toList();

		return loggingConsumers;
	}

	/**
	 * Returns the methods of the given bean class.
	 * @param bean the bean instance
	 * @return the methods of the bean class
	 */
	protected Method[] doGetClassMethods(Object bean) {

		Method[] methods = bean.getClass().getDeclaredMethods();
		Arrays.sort(methods, Comparator
				.comparing(Method::getName)
				.thenComparing(method -> Arrays.toString(method.getParameterTypes())));
		return methods;
	}


}
