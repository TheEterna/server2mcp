package com.ai.plug.common.utils;

import io.modelcontextprotocol.util.Assert;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * Utilities to perform parsing operations between JSON and Java.
 * <p>
 * Jackson 3 迁移说明（2026-07-30）：Spring AI 2.0 / MCP SDK 2.0 要求 Jackson 3，包根由
 * {@code com.fasterxml.jackson} 变为 {@code tools.jackson}；原 checked 异常
 * {@code JsonProcessingException} 由 {@link JacksonException} 取代，且后者继承
 * {@link RuntimeException}（unchecked）。方法签名保留 {@code throws} 声明以维持原有 API 形状，
 * 但调用方不再被强制捕获。
 */
public final class JsonParser {

	public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
		.addModules(JacksonUtils.instantiateAvailableModules())
		.build();

	private JsonParser() {
	}

	/**
	 * Returns a Jackson {@link ObjectMapper} instance tailored for JSON-parsing
	 * operations for tool calling and structured output.
	 */
	public static ObjectMapper getObjectMapper() {
		return OBJECT_MAPPER;
	}

	/**
	 * Converts a JSON string to a Java object.
	 */
	public static <T> T fromJson(String json, Class<T> type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			return OBJECT_MAPPER.readValue(json, type);
		}
		catch (JacksonException ex) {
			throw new IllegalStateException("Conversion from JSON to %s failed".formatted(type.getName()), ex);
		}
	}

	/**
	 * Converts a JSON string to a Java object.
	 */
	public static <T> T fromJson(String json, Type type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.constructType(type));
		}
		catch (JacksonException ex) {
			throw new IllegalStateException("Conversion from JSON to %s failed".formatted(type.getTypeName()), ex);
		}
	}

	/**
	 * Converts a JSON string to a Java object.
	 */
	public static <T> T fromJson(String json, TypeReference<T> type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			return OBJECT_MAPPER.readValue(json, type);
		}
		catch (JacksonException ex) {
			throw new IllegalStateException("Conversion from JSON to %s failed".formatted(type.getType().getTypeName()),
					ex);
		}
	}

	/**
	 * Checks if a string is a valid JSON string.
	 */
	private static boolean isValidJson(String input) {
		try {
			OBJECT_MAPPER.readTree(input);
			return true;
		}
		catch (JacksonException e) {
			return false;
		}
	}

	/**
	 * Converts a Java object to a JSON string if it's not already a valid JSON string.
	 */
	public static String toJson(@Nullable Object object) throws JacksonException {
		if (object == null) {
			return "[DONE]";
		}
		if (object instanceof String && isValidJson((String) object)) {
			return (String) object;
		}
		return OBJECT_MAPPER.writeValueAsString(object);

	}

	/**
	 * Convert a Java Object to a typed Object. Based on the implementation in
	 * MethodToolCallback.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object toTypedObject(Object value, Class<?> type) throws JacksonException {
		Assert.notNull(value, "value cannot be null");
		Assert.notNull(type, "type cannot be null");

		var javaType = ClassUtils.resolvePrimitiveIfNecessary(type);

		if (javaType == String.class) {
			return value.toString();
		}
		else if (javaType == Byte.class) {
			return Byte.parseByte(value.toString());
		}
		else if (javaType == Integer.class) {
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			return bigDecimal.intValueExact();
		}
		else if (javaType == Short.class) {
			return Short.parseShort(value.toString());
		}
		else if (javaType == Long.class) {
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			return bigDecimal.longValueExact();
		}
		else if (javaType == Double.class) {
			return Double.parseDouble(value.toString());
		}
		else if (javaType == Float.class) {
			return Float.parseFloat(value.toString());
		}
		else if (javaType == Boolean.class) {
			return Boolean.parseBoolean(value.toString());
		}
		else if (javaType.isEnum()) {
			return Enum.valueOf((Class<Enum>) javaType, value.toString());
		}

		String json = JsonParser.toJson(value);
		return JsonParser.fromJson(json, javaType);
	}

}
