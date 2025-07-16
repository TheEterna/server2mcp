package com.ai.plug.core.utils;

import com.ai.plug.core.annotation.McpArg;
import com.ai.plug.core.annotation.McpPrompt;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for adapting between McpPrompt annotations and McpSchema.Prompt objects.
 *
 * @author Christian Tzolov
 */
public class PromptAdaptor {

	private PromptAdaptor() {
	}

	/**
	 * Convert a McpPrompt annotation to a McpSchema.Prompt object.
	 * @param mcpPrompt The McpPrompt annotation
	 * @return The corresponding McpSchema.Prompt object
	 */
	public static McpSchema.Prompt asPrompt(McpPrompt mcpPrompt) {
		return new McpSchema.Prompt(mcpPrompt.name(), mcpPrompt.description(), List.of());
	}

	/**
	 * Convert a McpPrompt annotation to a McpSchema.Prompt object, including argument
	 * information from the method parameters.
	 * @param mcpPrompt The McpPrompt annotation
	 * @param method The method annotated with McpPrompt
	 * @return The corresponding McpSchema.Prompt object with argument information
	 */
	public static McpSchema.Prompt asPrompt(McpPrompt mcpPrompt, Method method) {
		List<McpSchema.PromptArgument> arguments = extractPromptArguments(method);
		return new McpSchema.Prompt(getName(mcpPrompt, method), mcpPrompt.description(), arguments);
	}

	private static String getName(McpPrompt promptAnnotation, Method method) {
		Assert.notNull(method, "method cannot be null");
		if (promptAnnotation == null || (promptAnnotation.name() == null)) {
			return method.getName();
		}
		return promptAnnotation.name();
	}

	/**
	 * Extract prompt arguments from a method's parameters.
	 * @param method The method to extract arguments from
	 * @return A list of PromptArgument objects
	 */
	private static List<McpSchema.PromptArgument> extractPromptArguments(Method method) {
		List<McpSchema.PromptArgument> arguments = new ArrayList<>();
		Parameter[] parameters = method.getParameters();

		for (Parameter parameter : parameters) {
			// Skip special parameter types
			if (McpAsyncServerExchange.class.isAssignableFrom(parameter.getType())
					|| McpSchema.GetPromptRequest.class.isAssignableFrom(parameter.getType())
					|| java.util.Map.class.isAssignableFrom(parameter.getType())) {
				continue;
			}

			// Check if parameter has McpArg annotation
			McpArg mcpArg = parameter.getAnnotation(McpArg.class);
			if (mcpArg != null) {
				String name = !mcpArg.name().isEmpty() ? mcpArg.name() : parameter.getName();
				arguments.add(new McpSchema.PromptArgument(name, mcpArg.description(), mcpArg.required()));
			}
			else {
				// Use parameter name and default values if no annotation
				arguments.add(new McpSchema.PromptArgument(parameter.getName(),
						"Parameter of type " + parameter.getType().getSimpleName(), false));
			}
		}

		return arguments;
	}

	/**
	 * Helper interface to avoid direct dependency on McpAsyncServerExchange in this
	 * package.
	 */
	private interface McpAsyncServerExchange {

	}

}
