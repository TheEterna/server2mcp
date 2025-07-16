package com.ai.plug.core.utils;

import com.ai.plug.core.annotation.McpArg;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import static com.ai.plug.core.utils.GenSchemaUtils.MCP_SCHEMA_GENERATOR;
import static com.ai.plug.core.utils.GenSchemaUtils.TOOL_SCHEMA_GENERATOR;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class GenSchemaUtilsTest {



    @Test
    public void testMcpSchemaGenerator() throws NoSuchMethodException {
//        Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
//        Module swagger3Module = new Swagger2Module();
//        Module swagger2Module = new SwaggerModule();
//
//        // 基本就很少用了，因为一般都是接口作为工具
//        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder =
//                (new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON))
//                        .with(jacksonModule)
//                        .with(swagger2Module)
//                        .with(swagger3Module)
//                        .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
//                        .with(Option.PLAIN_DEFINITION_KEYS)
//
//                ;
//
//        SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder
//                .without(Option.SCHEMA_VERSION_INDICATOR)
//                .build();
//        SchemaGenerator TOOL_SCHEMA_GENERATOR = new SchemaGenerator(subtypeSchemaGeneratorConfig);
        Method demoTest = this.getClass().getMethod("demoTest", Dto.class);
        Type parameterType = demoTest.getGenericParameterTypes()[0];
        ObjectNode schema = MCP_SCHEMA_GENERATOR.generateSchema(parameterType);

        System.out.println(schema.toPrettyString());
//        HashMap<String, Object> map = new HashMap<>();
//        map.put("type", schema.get("type"));
//        map.put("properties", schema.get("properties"));
//        map.put("required", schema.get("required"));
        Map<String, Object> map = GenSchemaUtils.objectNodeToMap(schema);
        System.out.println(map);
    }

    public void demoTest(@McpArg(description = "name11", required = true) Dto name) {

    }
    public class Dto {
        @McpArg(name = "nubu", description = "id", required = true)
        public Integer id;

        @Schema(name = "name1", description = "score", required = true)
        public String score;

        public Dto(Integer id, String score) {
            this.id = id;
            this.score = score;
        }


        public Dto() {
        }
}


    @Test
    public void testSingletonBehavior() {
        // Verify both generators are singletons and different instances
        assertSame(TOOL_SCHEMA_GENERATOR, TOOL_SCHEMA_GENERATOR);
        assertSame(MCP_SCHEMA_GENERATOR, MCP_SCHEMA_GENERATOR);
        assertNotSame(TOOL_SCHEMA_GENERATOR, MCP_SCHEMA_GENERATOR);
    }
}