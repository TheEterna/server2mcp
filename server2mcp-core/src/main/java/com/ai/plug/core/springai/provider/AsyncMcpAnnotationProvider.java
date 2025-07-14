/*
* Copyright 2025 - 2025 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ai.plug.core.springai.provider;

import com.ai.plug.core.builder.ToolDefinitionBuilder;
import com.ai.plug.core.context.ToolContext;
import com.ai.plug.core.context.root.IRootContext;
import com.ai.plug.core.provider.McpToolProvider;
import com.ai.plug.core.provider.*;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncCompletionSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Christian Tzolov
 */
public class AsyncMcpAnnotationProvider {

//	private static class SpringAiAsyncMcpLoggingConsumerProvider extends AsyncMcpLoggingConsumerProvider {
//
//		public SpringAiAsyncMcpLoggingConsumerProvider(List<Object> loggingObjects) {
//			super(loggingObjects);
//		}
//
//		@Override
//		protected Method[] doGetClassMethods(Object bean) {
//			return ReflectionUtils
//				.getDeclaredMethods(AopUtils.isAopProxy(bean) ? AopUtils.getTargetClass(bean) : bean.getClass());
//		}
//
//	}
//
//	private static class SpringAiAsyncMcpSamplingProvider extends AsyncMcpSamplingProvider {
//
//		public SpringAiAsyncMcpSamplingProvider(List<Object> samplingObjects) {
//			super(samplingObjects);
//		}
//
//		@Override
//		protected Method[] doGetClassMethods(Object bean) {
//			return ReflectionUtils
//					.getDeclaredMethods(AopUtils.isAopProxy(bean) ? AopUtils.getTargetClass(bean) : bean.getClass());
//		}
//
//	}
	private static class SpringAiAsyncMcpToolProvider extends McpToolProvider {
		public SpringAiAsyncMcpToolProvider(Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions, ToolDefinitionBuilder toolDefinitionBuilder, IRootContext rootContext) {
			super(toolAndDefinitions, toolDefinitionBuilder, rootContext);
		}

		@Override
		protected Method[] doGetClassMethods(Object bean) {
			Method[] methods = ReflectionUtils
					.getDeclaredMethods(AopUtils.isAopProxy(bean) ? AopUtils.getTargetClass(bean) : bean.getClass());

			Arrays.sort(methods, Comparator
					.comparing(Method::getName)
					.thenComparing(method -> Arrays.toString(method.getParameterTypes())));
			return methods;
		}


	}
	private static class SpringAiAsyncMcpResourceProvider extends McpResourceProvider {

		public SpringAiAsyncMcpResourceProvider(List<Object> resourceObjects) {
			super(resourceObjects);
		}

		@Override
		protected Method[] doGetClassMethods(Object bean) {
			Method[] methods = ReflectionUtils
					.getDeclaredMethods(AopUtils.isAopProxy(bean) ? AopUtils.getTargetClass(bean) : bean.getClass());
			Arrays.sort(methods, Comparator
					.comparing(Method::getName)
					.thenComparing(method -> Arrays.toString(method.getParameterTypes())));
			return methods;
		}

	}

	private static class SpringAiAsyncMcpPromptProvider extends McpPromptProvider {

		public SpringAiAsyncMcpPromptProvider(List<Object> promptObjects) {
			super(promptObjects);
		}

		@Override
		protected Method[] doGetClassMethods(Object bean) {
			Method[] methods = ReflectionUtils
					.getDeclaredMethods(AopUtils.isAopProxy(bean) ? AopUtils.getTargetClass(bean) : bean.getClass());
			Arrays.sort(methods, Comparator
					.comparing(Method::getName)
					.thenComparing(method -> Arrays.toString(method.getParameterTypes())));
			return methods;
		}

	}

	private static class SpringAiAsyncMcpCompletionProvider extends McpCompletionProvider {

		public SpringAiAsyncMcpCompletionProvider(List<Object> completionObjects) {
			super(completionObjects);
		}

		@Override
		protected Method[] doGetClassMethods(Object bean) {
			Method[] methods = ReflectionUtils
					.getDeclaredMethods(AopUtils.isAopProxy(bean) ? AopUtils.getTargetClass(bean) : bean.getClass());
			Arrays.sort(methods, Comparator
					.comparing(Method::getName)
					.thenComparing(method -> Arrays.toString(method.getParameterTypes())));
			return methods;
		}

	}

//	/**
//	 * fixme 未更新, 存在问题, 同步提供器无法解析Mono
//	 * @param loggingObjects
//	 * @return
//	 */
//	public static List<Function<LoggingMessageNotification, Mono<Void>>> createAsyncLoggingConsumers(
//			List<Object> loggingObjects) {
//		return new SpringAiAsyncMcpLoggingConsumerProvider(loggingObjects).getLoggingConsumers();
//	}
//
//	/**
//	 * fixme 未更新, 存在问题, 同步提供器无法解析Mono
//	 * @param samplingObjects
//	 * @return
//	 */
//
//	public static Function<CreateMessageRequest, Mono<CreateMessageResult>> createAsyncSamplingHandler(
//			List<Object> samplingObjects) {
//		return new SpringAiAsyncMcpSamplingProvider(samplingObjects).getSamplingHandler();
//	}


	public static List<AsyncResourceSpecification> createAsyncResourceSpecifications(List<Object> resourceObjects) {
		return new SpringAiAsyncMcpResourceProvider(resourceObjects).getAsyncResourceSpecifications();
	}

	public static List<AsyncCompletionSpecification> createAsyncCompletionSpecifications(List<Object> completionObjects) {
		return new SpringAiAsyncMcpCompletionProvider(completionObjects).getAsyncCompleteSpecifications();
	}

	public static List<AsyncPromptSpecification> createAsyncPromptSpecifications(List<Object> promptObjects) {
		return new SpringAiAsyncMcpPromptProvider(promptObjects).getAsyncPromptSpecifications();
	}

	public static List<AsyncToolSpecification> createAsyncToolSpecifications(Map<Object, ToolContext.ToolRegisterDefinition> toolAndDefinitions, ToolDefinitionBuilder builder, IRootContext rootContext) {
		return new SpringAiAsyncMcpToolProvider(toolAndDefinitions, builder, rootContext).getAsyncToolSpecifications();
	}


}
