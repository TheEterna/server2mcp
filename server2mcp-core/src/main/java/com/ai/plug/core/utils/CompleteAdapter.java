package com.ai.plug.core.utils;

import com.ai.plug.core.annotation.McpComplete;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;

import java.lang.reflect.Method;

/**
 * Utility class for adapting between McpComplete annotations and
 * McpSchema.CompleteReference objects.
 *
 * @author Christian Tzolov
 */
public class CompleteAdapter {

	private CompleteAdapter() {
	}

	/**
	 * Convert a McpComplete annotation to a McpSchema.CompleteReference object.
	 * @param mcpComplete The McpComplete annotation
	 * @return The corresponding McpSchema.CompleteReference object
	 * @throws IllegalArgumentException if neither prompt nor uri is provided, or if both
	 * are provided
	 */
	public static McpSchema.CompleteReference asCompleteReference(McpComplete mcpComplete) {
		Assert.notNull(mcpComplete, "mcpComplete cannot be null");

		String prompt = mcpComplete.prompt();
		String uri = mcpComplete.uri();

		// Validate that either prompt or uri is provided, but not both
		if ((prompt == null || prompt.isEmpty()) && (uri == null || uri.isEmpty())) {
			throw new IllegalArgumentException("Either prompt or uri must be provided in McpComplete annotation");
		}
		if ((prompt != null && !prompt.isEmpty()) && (uri != null && !uri.isEmpty())) {
			throw new IllegalArgumentException("Only one of prompt or uri can be provided in McpComplete annotation");
		}

		// Create the appropriate reference type based on what's provided
		if (prompt != null && !prompt.isEmpty()) {
			return new McpSchema.PromptReference(prompt);
		}
		else {
			return new McpSchema.ResourceReference(uri);
		}
	}

	/**
	 * Convert a McpComplete annotation and Method to a McpSchema.CompleteReference
	 * object.
	 * @param mcpComplete The McpComplete annotation
	 * @param method The method annotated with McpComplete
	 * @return The corresponding McpSchema.CompleteReference object
	 * @throws IllegalArgumentException if neither prompt nor uri is provided, or if both
	 * are provided
	 */
	public static McpSchema.CompleteReference asCompleteReference(McpComplete mcpComplete, Method method) {
		Assert.notNull(method, "method cannot be null");
		return asCompleteReference(mcpComplete);
	}

}