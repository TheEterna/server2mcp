/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.spec.callback.resource;

import com.ai.plug.core.spec.callback.resource.AbstractMcpResourceMethodCallback.ContentType;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;

/**
 * Interface for converting method return values to {@link ReadResourceResult}.
 * <p>
 * This interface defines a contract for converting various return types from resource
 * methods to a standardized {@link ReadResourceResult} format.
 *
 * @author Christian Tzolov
 */
public interface McpReadResourceResultConverter {

	/**
	 * Converts the method's return value to a {@link ReadResourceResult}.
	 * <p>
	 * This method handles various return types and converts them to a standardized
	 * {@link ReadResourceResult} format.
	 * @param result The method's return value
	 * @param requestUri The original request URI
	 * @param mimeType The MIME type of the resource
	 * @param contentType The content type of the resource
	 * @return A {@link ReadResourceResult} containing the appropriate resource contents
	 * @throws IllegalArgumentException if the return type is not supported
	 */
	ReadResourceResult convertToReadResourceResult(Object result, String requestUri, String mimeType,
			ContentType contentType);

}
