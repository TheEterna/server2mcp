package com.ai.plug.core.spec;

import com.ai.plug.core.utils.GenSchemaUtils;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author han
 * @time 2025/7/8 1:24
 */

public class ElicitationTests {

    @Test
    public void testElicitation() {
        // 测试String类型的schema生成
        ObjectNode stringSchema = GenSchemaUtils.MCP_SCHEMA_GENERATOR.generateSchema(String.class);
        System.out.println("String Schema: " + stringSchema.toPrettyString());
        
        Map<String, Object> stringSchemaMap = GenSchemaUtils.objectNodeToMap(stringSchema);
        System.out.println("String Schema Map: " + stringSchemaMap);
    }

}
