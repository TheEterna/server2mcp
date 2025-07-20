/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.annotation;

import java.lang.annotation.*;

/**
 * Marks a method parameter as a MCP Argument.
 * 在 resource 和 prompt 功能里可以作为参数
 * 在Tool里可以标识为mcp的参数, 比如exchange 和 上下文Map(由于类型擦除, 无法准确注入, 故使用该注解)
 * @author Christian Tzolov
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpArg {

	/**
	 * Argument name.
	 */
	String name() default "";

	/**
	 * Argument description.
	 */
	String description() default "";

	/**
	 * True if this argument is required. false if this argument is optional.
	 */
	boolean required() default false;

}
