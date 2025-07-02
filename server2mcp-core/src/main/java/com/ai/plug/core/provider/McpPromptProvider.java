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

import com.ai.plug.core.spec.callback.prompt.AsyncMcpPromptMethodCallback;
import com.ai.plug.core.spec.callback.prompt.SyncMcpPromptMethodCallback;
import com.logaritex.mcp.annotation.McpPrompt;
import com.logaritex.mcp.annotation.PromptAdaptor;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.util.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 */
public class McpPromptProvider {

	private final List<Object> promptObjects;

	public McpPromptProvider(List<Object> promptObjects) {
		Assert.notNull(promptObjects, "promptObjects cannot be null");
		this.promptObjects = promptObjects;
	}

	public List<AsyncPromptSpecification> getAsyncPromptSpecifications() {

		List<AsyncPromptSpecification> asyncPromptSpecification = this.promptObjects.stream()
			.map(promptObjects -> Stream.of(doGetClassMethods(promptObjects))
				.filter(method -> method.isAnnotationPresent(McpPrompt.class))
				.map(mcpPromptMethod -> {
					var promptAnnotation = mcpPromptMethod.getAnnotation(McpPrompt.class);
					var mcpPrompt = PromptAdaptor.asPrompt(promptAnnotation, mcpPromptMethod);

					AsyncMcpPromptMethodCallback asyncMcpPromptMethodCallback = AsyncMcpPromptMethodCallback.builder()
						.method(mcpPromptMethod)
						.bean(promptObjects)
						.prompt(mcpPrompt)
						.build();

					return new AsyncPromptSpecification(mcpPrompt, asyncMcpPromptMethodCallback);
				})
				.toList())
			.flatMap(List::stream)
			.toList();

		return asyncPromptSpecification;
	}
	public List<SyncPromptSpecification> getSyncPromptSpecifications() {

		List<SyncPromptSpecification> syncPromptSpecification = this.promptObjects.stream()
			.map(promptObjects -> Stream.of(doGetClassMethods(promptObjects))
				.filter(method -> method.isAnnotationPresent(McpPrompt.class))
				.map(mcpPromptMethod -> {
					var promptAnnotation = mcpPromptMethod.getAnnotation(McpPrompt.class);
					var mcpPrompt = PromptAdaptor.asPrompt(promptAnnotation, mcpPromptMethod);

					SyncMcpPromptMethodCallback syncMcpPromptMethodCallback = SyncMcpPromptMethodCallback.builder()
						.method(mcpPromptMethod)
						.bean(promptObjects)
						.prompt(mcpPrompt)
						.build();

					return new SyncPromptSpecification(mcpPrompt, syncMcpPromptMethodCallback);
				})
				.toList())
			.flatMap(List::stream)
			.toList();

		return syncPromptSpecification;
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
