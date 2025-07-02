/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.ai.plug.core.spec.callback.resource;

import com.ai.plug.core.spec.callback.resource.AbstractMcpResourceMethodCallback.ContentType;
import io.modelcontextprotocol.spec.McpSchema.BlobResourceContents;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link McpReadResourceResultConverter}.
 * <p>
 * This class provides a standard implementation for converting various return types from
 * resource methods to a standardized {@link ReadResourceResult} format.
 *
 * @author Christian Tzolov
 */
public class DefaultMcpReadResourceResultConverter implements McpReadResourceResultConverter {

	/**
	 * Default MIME type to use when none is specified.
	 */
	private static final String DEFAULT_MIME_TYPE = "text/plain";

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
	@Override
	public ReadResourceResult convertToReadResourceResult(Object result, String requestUri, String mimeType,
			ContentType contentType) {
		if (result == null) {
			return new ReadResourceResult(List.of());
		}

		if (result instanceof ReadResourceResult) {
			return (ReadResourceResult) result;
		}

		mimeType = (mimeType != null && !mimeType.isEmpty()) ? mimeType : DEFAULT_MIME_TYPE;

		// Determine content type from mime type since contentType() was moved from
		// McpResource
		contentType = contentType != null ? contentType
				: isTextMimeType(mimeType) ? ContentType.TEXT : ContentType.BLOB;

		List<ResourceContents> contents;

		if (result instanceof List<?>) {
			contents = convertListResult((List<?>) result, requestUri, contentType, mimeType);
		}
		else if (result instanceof ResourceContents) {
			// Single ResourceContents
			contents = List.of((ResourceContents) result);
		}
		else if (result instanceof String) {
			// Single String -> ResourceContents (TextResourceContents or
			// BlobResourceContents)
			contents = convertStringResult((String) result, requestUri, contentType, mimeType);
		}
		else {
			throw new IllegalArgumentException("Unsupported return type: " + result.getClass().getName());
		}

		return new ReadResourceResult(contents);
	}

	private boolean isTextMimeType(String mimeType) {
		if (mimeType == null) {
			return false;
		}

		// Direct text types
		if (mimeType.startsWith("text/")) {
			return true;
		}

		// Common text-based MIME types that don't start with "text/"
		return mimeType.equals("application/json") || mimeType.equals("application/xml")
				|| mimeType.equals("application/javascript") || mimeType.equals("application/ecmascript")
				|| mimeType.equals("application/x-httpd-php") || mimeType.equals("application/xhtml+xml")
				|| mimeType.endsWith("+json") || mimeType.endsWith("+xml");
	}

	/**
	 * Converts a List result to a list of ResourceContents.
	 * @param list The list result
	 * @param requestUri The original request URI
	 * @param contentType The content type (TEXT or BLOB)
	 * @param mimeType The MIME type
	 * @return A list of ResourceContents
	 * @throws IllegalArgumentException if the list item type is not supported
	 */
	@SuppressWarnings("unchecked")
	private List<ResourceContents> convertListResult(List<?> list, String requestUri, ContentType contentType,
			String mimeType) {
		if (list.isEmpty()) {
			return List.of();
		}

		Object firstItem = list.get(0);

		if (firstItem instanceof ResourceContents) {
			// List<ResourceContents>
			return (List<ResourceContents>) list;
		}
		else if (firstItem instanceof String) {
			// List<String> -> List<ResourceContents> (TextResourceContents or
			// BlobResourceContents)
			List<String> stringList = (List<String>) list;
			List<ResourceContents> result = new ArrayList<>(stringList.size());

			if (contentType == ContentType.TEXT) {
				for (String text : stringList) {
					result.add(new TextResourceContents(requestUri, mimeType, text));
				}
			}
			else { // BLOB
				for (String blob : stringList) {
					result.add(new BlobResourceContents(requestUri, mimeType, blob));
				}
			}

			return result;
		}
		else {
			throw new IllegalArgumentException("Unsupported list item type: " + firstItem.getClass().getName()
					+ ". Expected String or ResourceContents.");
		}
	}

	/**
	 * Converts a String result to a list of ResourceContents.
	 * @param stringResult The string result
	 * @param requestUri The original request URI
	 * @param contentType The content type (TEXT or BLOB)
	 * @param mimeType The MIME type
	 * @return A list containing a single ResourceContents
	 */
	private List<ResourceContents> convertStringResult(String stringResult, String requestUri, ContentType contentType,
			String mimeType) {
		if (contentType == ContentType.TEXT) {
			return List.of(new TextResourceContents(requestUri, mimeType, stringResult));
		}
		else { // BLOB
			return List.of(new BlobResourceContents(requestUri, mimeType, stringResult));
		}
	}

}
