package com.ai.plug.autoconfigure;

import com.ai.plug.core.annotation.ToolScan;
import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.core.ToolContext;
import com.ai.plug.core.parser.starter.SingleStarter;
import com.ai.plug.core.parser.starter.Starter;
import com.ai.plug.core.provider.CustomToolCallbackProvider;
import com.ai.plug.core.register.ToolScanConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ai.plug.core.annotation.ToolScan.FilterType.ANNOTATION;
import static com.ai.plug.core.annotation.ToolScan.ToolFilterType.META_ANNOTATION;
import static com.ai.plug.common.constants.ConfigConstants.VARIABLE_PREFIX;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "com.ai.plug")
@Conditional(Conditions.SystemCondition.class)
@Import({Server2McpAutoConfiguration.AutoConfiguredToolScannerRegistrar.class, ParserConfig.class})
public class Server2McpAutoConfiguration {
    public static final Logger logger = LoggerFactory.getLogger(Server2McpAutoConfiguration.class);


    @Bean
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX, name = ".enabled", havingValue = "true")
    @Primary
    // 如果扫描区域为接口
    public ToolCallbackProvider customToolCallbackProvider(ApplicationContext applicationContext, PluginProperties properties){
        Map<Object, ToolContext.ToolRegisterDefinition> tools = ToolContext.getRawTools().entrySet().stream().collect(Collectors.toMap(
                entry -> applicationContext.getBean(entry.getKey()),
                Map.Entry::getValue
        ));
        // 需要在这里添加被@ToolScan注解scan到的bean
        return CustomToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
    @Bean
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX, name = ".enabled", havingValue = "true")
    public Starter starter() {
        return new SingleStarter();
    }




    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> myResources() {

        McpSchema.Resource systemInfoResource = new McpSchema.Resource("https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html#_resource_management"
        , "mcp文档"
        , "这是一个springboot对mcp协议集成的文档"
        , "plain/txt"
        , new McpSchema.Annotations(Collections.singletonList(McpSchema.Role.ASSISTANT), 0.99));

        McpServerFeatures.SyncResourceSpecification resourceSpecification = new McpServerFeatures.SyncResourceSpecification(
                systemInfoResource,
                (exchange, request) -> {
            try {
                var systemInfo = Map.of("account", "3168134942", "password", "123456");
                String jsonContent = new ObjectMapper().writeValueAsString(systemInfo);
                return new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", jsonContent))
                );
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to generate system info", e);
            }
        });

        return List.of(resourceSpecification);
    }


    @Configuration(proxyBeanMethods = false)
    @Conditional(Conditions.IsInterfaceCondition.class)
    public static class AutoConfiguredToolScannerRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
        private BeanFactory beanFactory;

        // 无参构造器
        public AutoConfiguredToolScannerRegistrar() {
        }

        public BeanFactory getBeanFactory() {
            return beanFactory;
        }

        public void setBeanFactory(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        // 注册BeanDefinitions
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            if (!AutoConfigurationPackages.has(this.beanFactory)) {
                Server2McpAutoConfiguration.logger.debug("Could not determine auto-configuration package, automatic mapper scanning disabled.");
                return;
            }
            Server2McpAutoConfiguration.logger.debug("Searching for tools of interfaces");

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ToolScanConfigurer.class);

            String[] basePackages = AutoConfigurationPackages.get(this.beanFactory).toArray(new String[0]);
            builder.addPropertyValue("basePackages", basePackages);

            HashMap<String, Object> excludeMap = new HashMap<>();
            excludeMap.put("classes", Deprecated.class);
            excludeMap.put("type", ANNOTATION);
            excludeMap.put("value", Deprecated.class);
            AnnotationAttributes[] excludeFilters = new AnnotationAttributes[]{new AnnotationAttributes(excludeMap)};
            builder.addPropertyValue("excludeFilters", excludeFilters);

            HashMap<String, Object> includeMap = new HashMap<>();
            includeMap.put("classes", Controller.class);
            includeMap.put("type", ANNOTATION);
            includeMap.put("value", Controller.class);
            AnnotationAttributes[] includeFilters = new AnnotationAttributes[]{new AnnotationAttributes(includeMap)};
            builder.addPropertyValue("includeFilters", includeFilters);


            HashMap<String, Object> excludeToolMap = new HashMap<>();
            excludeToolMap.put("classes", Deprecated.class);
            excludeToolMap.put("type", META_ANNOTATION);
            excludeToolMap.put("value", Deprecated.class);
            ToolScan.ToolFilter[] excludeToolFilters= convertToToolFilters(new AnnotationAttributes[]{new AnnotationAttributes(excludeToolMap)});
            builder.addPropertyValue("excludeToolFilters", excludeToolFilters);

            HashMap<String, Object> includeToolMap = new HashMap<>();
            includeToolMap.put("classes", RequestMapping.class);
            includeToolMap.put("type", META_ANNOTATION);
            includeToolMap.put("value", RequestMapping.class);
            ToolScan.ToolFilter[] includeToolFilters = convertToToolFilters(new AnnotationAttributes[]{new AnnotationAttributes(includeToolMap)});
            builder.addPropertyValue("includeToolFilters", includeToolFilters);


            builder.setRole(2);
            registry.registerBeanDefinition(ToolScanConfigurer.class.getName(), builder.getBeanDefinition());

        }
        private ToolScan.ToolFilter[] convertToToolFilters(AnnotationAttributes[] attributesArray) {
            if (attributesArray == null) {
                return new ToolScan.ToolFilter[0];
            }
            ToolScan.ToolFilter[] toolFilters = new ToolScan.ToolFilter[attributesArray.length];
            for (int i = 0; i < attributesArray.length; i++) {
                AnnotationAttributes attributes = attributesArray[i];
                toolFilters[i] = new ToolScan.ToolFilter() {
                    @Override
                    public ToolScan.ToolFilterType type() {
                        return attributes.getEnum("type");
                    }

                    @Override
                    public Class<?>[] value() {
                        return attributes.getClassArray("value");
                    }

                    @Override
                    public Class<?>[] classes() {
                        return attributes.getClassArray("classes");
                    }

                    @Override
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return ToolScan.ToolFilter.class;
                    }
                };
            }
            return toolFilters;
        }


    }

}