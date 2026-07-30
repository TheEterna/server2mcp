/*
 * Copyright 2023-2024 the original author or authors.
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

package com.ai.plug.common.utils;

import tools.jackson.databind.JacksonModule;

import java.util.List;

/**
 * Utility methods for Jackson.
 * <p>
 * 本类原为 Spring AI {@code org.springframework.ai.util.JacksonUtils} 的复制品。自 Spring AI
 * 2.0（MCP SDK 2.0 / Jackson 3）起，官方实现已适配 Jackson 3 生态——Jackson 3 把
 * {@code Jdk8Module}、{@code JavaTimeModule}、{@code ParameterNamesModule} 并入核心，原先按
 * Jackson 2 类名反射加载的逻辑在 Jackson 3 下会全部落空并静默返回空列表（时间/JDK8 类型序列化退化）。
 * 故此处收口为委托官方实现，既消除重复代码，也随官方跟进 Jackson 生态变化。
 * <p>
 * 公开 API 形状保持不变（类名、方法名、静态调用方式），仅元素类型随 Jackson 3 由
 * {@code com.fasterxml.jackson.databind.Module} 变为 {@link JacksonModule}。
 *
 * @author Sebastien Deleuze
 */
public abstract class JacksonUtils {

	/**
	 * Instantiate well-known Jackson modules available in the classpath.
	 * @return The list of instantiated modules.
	 */
	public static List<JacksonModule> instantiateAvailableModules() {
		return org.springframework.ai.util.JacksonUtils.instantiateAvailableModules();
	}

}
