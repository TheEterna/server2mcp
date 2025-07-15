/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.annotation;

import java.lang.annotation.*;

/**
 * Marks a method as a MCP Prompt.
 *
 * @author Christian Tzolov
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpPrompt {

	/**
	 * A human-readable name for this resource. This can be used by clients to populate UI
	 * elements.
	 */
	String name() default "";

	/**
	 * 该资源的描述信息，用于帮助客户端提升LLM对可用资源的理解。可以将其视为给模型的一个“提示”。
	 */
	String description() default "";

}
