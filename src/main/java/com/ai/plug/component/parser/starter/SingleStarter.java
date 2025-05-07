package com.ai.plug.component.parser.starter;

import com.ai.plug.component.parser.des.AbstractDesParser;
import com.ai.plug.component.parser.param.AbstractParamParser;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.util.json.schema.SpringAiSchemaModule;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;


/**
 * @ author 韩
 * time: 2025/4/29 17:03
 */
@Component
@Primary
@Slf4j
public final class SingleStarter extends Starter {



    private static final SchemaGenerator SUBTYPE_SCHEMA_GENERATOR;
    static {
        Module springAiSchemaModule = new SpringAiSchemaModule();
        Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
        Module swagger3Module = new Swagger2Module();
        Module swagger2Module = new SwaggerModule();

        // 基本就很少用了，因为一般都是接口作为工具
        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder =
                (new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON))
                        .with(springAiSchemaModule)
                        .with(jacksonModule)
                        .with(swagger2Module)
                        .with(swagger3Module)
                        .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                        .with(Option.PLAIN_DEFINITION_KEYS);
        SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.without(Option.SCHEMA_VERSION_INDICATOR).build();
        SUBTYPE_SCHEMA_GENERATOR = new SchemaGenerator(subtypeSchemaGeneratorConfig);
    }

    @Override
    public String runDesParse(List<AbstractDesParser> parserList, Method method, Class<?> toolClass) {

        String result = parserList.stream().map((parser) -> {
            return parser.doDesParse(method, toolClass);
        }).filter(Objects::nonNull).findFirst().orElse(null);

        if (!StringUtils.hasText(result)) {
            log.warn("Please note that the parsing module you specified resulted in the {} tool not parsing the tool description", toolClass.getName() + "-" + method.getName());
            return AbstractDesParser.doDefaultParse(method, toolClass);
        }
        return result;

    }

    @Override
    public Boolean runParamRequiredParse(List<AbstractParamParser> parserList, Method method, Class<?> toolClass, int index) {

        Boolean result = parserList.stream().map((parser) -> {
            return parser.doParamRequiredParse(method, toolClass, index);
        }).filter(Objects::nonNull).findFirst().orElse(null);

        if (result == null) {
//            log.warn("Please note that the parsing module you specified resulted in the {} tool not parsing the param required", toolClass.getName() + "-" + method.getName());
            return AbstractParamParser.doDefaultParamRequiredParse(method, toolClass, index);
        }
        return result;
    }


}
