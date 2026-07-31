package com.ai.plug.core.spec.utils.elicitation;

import com.ai.plug.core.utils.GenSchemaUtils;
import tools.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.ai.plug.core.utils.GenSchemaUtils.MCP_SCHEMA_GENERATOR;

/**
 * 通过 exchange 向客户端发起 elicitation 请求。
 * <p>
 * MCP 协议 2025-11-25 起 elicitation 分为两种模式：
 * <ul>
 *   <li><b>form</b>：服务端定义 JSON Schema，客户端在表单中收集后回填；本项目默认走该模式。</li>
 *   <li><b>url</b>：服务端给出一个 URL，客户端引导用户到浏览器侧完成（OAuth、隐私表单等场景）。</li>
 * </ul>
 * SDK 2.0 把 {@link McpSchema.ElicitRequest} 改为接口，由 {@link McpSchema.ElicitFormRequest}
 * 与 {@link McpSchema.ElicitUrlRequest} 两个 record 实现。本接口相应地暴露两个 {@code default}
 * 构造方法，二者复用同一份「Java 类 → JSON Schema → protocol 强制 object 包装」转换逻辑。
 *
 * @author han
 * @time 2025/7/4 16:04
 */
public interface McpElicitation {

    /**
     * 构建 form 模式 elicitation 请求。
     * <p>
     * 注意：{@code McpSchema.ElicitRequest.builder()} 已 deprecated（Spring AI 2.0 编译器告警），
     * 但仍指向 {@link McpSchema.ElicitFormRequest.Builder}，为减少与 SDK 演进解耦的破坏面，保留
     * 调用点；后续 SDK 移除时再统一切到 {@code ElicitFormRequest.builder()} 直调。
     *
     * @param message 提示给用户的消息
     * @param schema  用于生成 requestedSchema 的 Java 类型
     * @return form 模式请求，协议要求 requestedSchema 是含 properties 的 object 类型
     */
    default McpSchema.ElicitFormRequest buildElicitationRequest(String message, Class<?> schema) {
        McpSchema.ElicitFormRequest.Builder builder = McpSchema.ElicitRequest.builder();
        builder.message(message);
        builder.requestedSchema(toRequestedSchema(schema));
        return builder.build();
    }

    /**
     * 构建 url 模式 elicitation 请求。SDK 2.0 URL 模式：服务端提供消息、URL 与服务端侧 ID，
     * 客户端引导用户去该 URL 完成后再带同一 ID 回来。服务端 ID 由本项目按 UUID 派生，调用方也可
     * 通过重载传入自己的 ID 以便关联业务状态。
     *
     * @param message 提示给用户的消息
     * @param url     客户端要跳转的 URL
     * @return url 模式请求
     */
    default McpSchema.ElicitUrlRequest buildElicitationUrlRequest(String message, String url) {
        return buildElicitationUrlRequest(message, url, java.util.UUID.randomUUID().toString());
    }

    /**
     * 构建 url 模式 elicitation 请求，调用方显式指定服务端侧 correlation ID。
     */
    default McpSchema.ElicitUrlRequest buildElicitationUrlRequest(String message, String url, String elicitationId) {
        return McpSchema.ElicitUrlRequest.builder(message, url, elicitationId).build();
    }

    /**
     * Java 类 → 协议所需的 requestedSchema。
     * <p>
     * 协议要求 requestedSchema 必须是含 {@code properties} 字段的 object 类型——非 object 类型的
     * Java 类会被包成单字段 {@code value}；已为 object 类型但缺 properties 的会补空 properties。
     * 转换通过本项目 victools 接入（{@link GenSchemaUtils#objectNodeToMap}），无任何调试打印。
     */
    private Map<String, Object> toRequestedSchema(Class<?> schema) {
        ObjectNode jsonSchema = MCP_SCHEMA_GENERATOR.generateSchema(schema);
        Map<String, Object> mapSchema = GenSchemaUtils.objectNodeToMap(jsonSchema);
        if (!"object".equals(mapSchema.get("type"))) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("value", mapSchema);
            Map<String, Object> wrappedSchema = new HashMap<>();
            wrappedSchema.put("type", "object");
            wrappedSchema.put("properties", properties);
            wrappedSchema.put("required", Collections.singletonList("value"));
            return wrappedSchema;
        }
        if (!mapSchema.containsKey("properties")) {
            mapSchema.put("properties", new HashMap<>());
        }
        return mapSchema;
    }

    /**
     * 通过 exchange 向客户端发起 form 模式 elicitation 请求。
     * @param message 请求消息
     * @param schema  反序列化目标类型
     * @return elicitation 结果
     */
    McpSchema.ElicitResult elicit(String message, Class<?> schema);

    /**
     * 异步版：{@link #elicit(String, Class)}。
     */
    Mono<McpSchema.ElicitResult> elicitAsync(String message, Class<?> schema);

}