/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.annotation;

import java.lang.annotation.*;

/**
 * Marks a method as a MCP Resource.
 *
 * @author Christian Tzolov
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpResource {

	/**
	 * A human-readable name for this resource. This can be used by clients to populate UI
	 * elements.
	 */
	String name() default "";

	String title() default "";

	/**
	 * the URI of the resource.
	 */
	String uri() default "";

	/**
	 * A description of what this resource represents. This can be used by clients to
	 * improve the LLM's understanding of available resources. It can be thought of like a
	 * "hint" to the model.
	 */
	String description() default "";

	/**
	 * The MIME type of this resource, if known.
	 */
	String mimeType() default "text/plain";

}
