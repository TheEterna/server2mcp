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

import com.logaritex.mcp.annotation.McpSampling;
import com.logaritex.mcp.method.sampling.AsyncMcpSamplingMethodCallback;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.util.Assert;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Provider for asynchronous sampling callbacks.
 *
 * <p>
 * This class scans a list of objects for methods annotated with {@link McpSampling} and
 * creates {@link Function} callbacks for them. These callbacks can be used to handle
 * sampling requests from MCP servers in a reactive way.
 *
 * <p>
 * Example usage: <pre>{@code
 * // Create a provider with a list of objects containing @McpSampling methods
 * AsyncMcpSamplingProvider provider = new AsyncMcpSamplingProvider(List.of(samplingHandler));
 *
 * // Get the sampling handler
 * Function<CreateMessageRequest, Mono<CreateMessageResult>> samplingHandler = provider.getSamplingHandler();
 *
 * // Add the handler to the client features
 * McpClientFeatures.Async clientFeatures = new McpClientFeatures.Async(
 *     clientInfo, clientCapabilities, roots,
 *     toolsChangeConsumers, resourcesChangeConsumers, promptsChangeConsumers,
 *     loggingConsumers, samplingHandler);
 * }</pre>
 *
 * @author Christian Tzolov
 * @see McpSampling
 * @see AsyncMcpSamplingMethodCallback
 * @see CreateMessageRequest
 * @see CreateMessageResult
 */
public class AsyncMcpSamplingProvider {

	private final List<Object> samplingObjects;

	/**
	 * Create a new AsyncMcpSamplingProvider.
	 * @param samplingObjects the objects containing methods annotated with
	 * {@link McpSampling}
	 */
	public AsyncMcpSamplingProvider(List<Object> samplingObjects) {
		Assert.notNull(samplingObjects, "samplingObjects cannot be null");
		this.samplingObjects = samplingObjects;
	}

	/**
	 * Get the sampling handler.
	 * @return the sampling handler
	 * @throws IllegalStateException if no sampling methods are found or if multiple
	 * sampling methods are found
	 */
	public Function<CreateMessageRequest, Mono<CreateMessageResult>> getSamplingHandler() {
		List<Function<CreateMessageRequest, Mono<CreateMessageResult>>> samplingHandlers = this.samplingObjects.stream()
			.map(samplingObject -> Stream.of(doGetClassMethods(samplingObject))
				.filter(method -> method.isAnnotationPresent(McpSampling.class))
				.filter(method -> method.getParameterCount() == 1
						&& CreateMessageRequest.class.isAssignableFrom(method.getParameterTypes()[0]))
				.filter(method -> Mono.class.isAssignableFrom(method.getReturnType())
						|| CreateMessageResult.class.isAssignableFrom(method.getReturnType()))
				.map(mcpSamplingMethod -> {
					var samplingAnnotation = mcpSamplingMethod.getAnnotation(McpSampling.class);

					Function<CreateMessageRequest, Mono<CreateMessageResult>> methodCallback = AsyncMcpSamplingMethodCallback
						.builder()
						.method(mcpSamplingMethod)
						.bean(samplingObject)
						.sampling(samplingAnnotation)
						.build();

					return methodCallback;
				})
				.toList())
			.flatMap(List::stream)
			.toList();

		if (samplingHandlers.isEmpty()) {
			throw new IllegalStateException("No sampling methods found");
		}
		if (samplingHandlers.size() > 1) {
			throw new IllegalStateException("Multiple sampling methods found: " + samplingHandlers.size());
		}

		return samplingHandlers.get(0);
	}

	/**
	 * Returns the methods of the given bean class.
	 * @param bean the bean instance
	 * @return the methods of the bean class
	 */
	protected Method[] doGetClassMethods(Object bean) {
		return bean.getClass().getDeclaredMethods();
	}

}
