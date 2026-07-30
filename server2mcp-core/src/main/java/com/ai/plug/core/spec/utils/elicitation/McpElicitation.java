package com.ai.plug.core.spec.utils.elicitation;

import com.ai.plug.core.utils.GenSchemaUtils;
import tools.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static com.ai.plug.core.utils.GenSchemaUtils.MCP_SCHEMA_GENERATOR;

/**
 * 通过 exchange 向 客户端 发起 elicitation 请求
 * @author han
 * @time 2025/7/4 16:04
 */

public interface McpElicitation {

    default McpSchema.ElicitRequest buildElicitationRequest(String message, Class<?> schema) {
        // 0. 完成组装 ElicitRequest 即可
        // 0 Complete the assembly of elicitrequest
        // MCP SDK 2.0（协议 2025-11-25）起 ElicitRequest 变为接口，按 form / url 两种模式分裂为
        // ElicitFormRequest 与 ElicitUrlRequest；ElicitRequest.builder() 返回的即 form 模式 Builder。
        McpSchema.ElicitFormRequest.Builder builder = McpSchema.ElicitRequest.builder();
        // 1.0 组装messgae部分
        // 1.0 assembling mesgae part
        builder.message(message);
        // 2.0. 首先需要理清一下逻辑, 不能简单的把一个类的字段直接转成schema, 需要考虑一下上面有没有 需要忽略的字段
        // 2.0. First, clarify the logic. It's not possible to directly convert a class's fields into a schema without considering whether there are any fields that need to be ignored.
        // 2.1. 然后, 使用victools 生成 JSONSchema
        // 2.1. Then use victools generate JSONSchema
        ObjectNode jsonSchema = MCP_SCHEMA_GENERATOR.generateSchema(schema);
        // 2.2 convert ObjectNode to map
        // 2.2 进行转换
        Map<String, Object> mapSchema = GenSchemaUtils.objectNodeToMap(jsonSchema);
        
        System.out.println("[DEBUG] Original schema for " + schema.getSimpleName() + ": " + mapSchema);
        
        // 2.3 检查并确保schema符合MCP协议要求
        // 2.3 Check and ensure schema conforms to MCP protocol requirements
        // MCP协议要求requestedSchema必须是object类型且包含properties字段
        // MCP protocol requires requestedSchema to be object type with properties field
        if (!"object".equals(mapSchema.get("type"))) {
            // 如果不是object类型,需要包装成object类型
            // If not object type, wrap it as object type
            Map<String, Object> wrappedSchema = new HashMap<>();
            wrappedSchema.put("type", "object");
            Map<String, Object> properties = new HashMap<>();
            properties.put("value", mapSchema);
            wrappedSchema.put("properties", properties);
            wrappedSchema.put("required", java.util.Collections.singletonList("value"));
            mapSchema = wrappedSchema;
            System.out.println("[DEBUG] Wrapped schema: " + mapSchema);
        } else if (!mapSchema.containsKey("properties")) {
            // 如果是object类型但没有properties字段,添加空的properties
            // If object type but no properties field, add empty properties
            mapSchema.put("properties", new HashMap<>());
            System.out.println("[DEBUG] Added empty properties to object schema");
        }
        
        System.out.println("[DEBUG] Final schema: " + mapSchema);
        
        builder.requestedSchema(mapSchema);
        // 3.0 进行构建获取 ElicitRequest
        // 3.0 Build to Obtain ElicitRequest
        McpSchema.ElicitRequest elicitRequest = builder.build();
        return elicitRequest;
    }
    /**
     * 通过 exchange 向 客户端 发起elicitation 请求, 指定反序列化的 格式
     * @param message 请求的消息
     * @param schema 反序列化的格式
     * @return McpSchema.ElicitResult
     */
    McpSchema.ElicitResult elicit(String message, Class<?> schema);

    /**
     * 通过 exchange 向 客户端 发起elicitation 请求, 指定反序列化的 格式
     * @param message 请求的消息
     * @param schema 反序列化的格式
     * @return McpSchema.ElicitResult
     */
    Mono<McpSchema.ElicitResult> elicitAsync(String message, Class<?> schema);


}
