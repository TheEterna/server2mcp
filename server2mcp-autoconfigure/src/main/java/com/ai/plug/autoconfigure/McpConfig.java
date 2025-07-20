package com.ai.plug.autoconfigure;

import com.ai.plug.autoconfigure.annotation.ToolNotScanForAuto;
import com.ai.plug.autoconfigure.conditional.ConditionalOnParser;
import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.core.annotation.McpCompleteScan;
import com.ai.plug.core.annotation.McpPromptScan;
import com.ai.plug.core.annotation.McpResourceScan;
import com.ai.plug.core.annotation.ToolScan;
import com.ai.plug.core.builder.ToolDefinitionBuilder;
import com.ai.plug.core.context.complete.CompleteContextFactory;
import com.ai.plug.core.context.complete.ICompleteContext;
import com.ai.plug.core.context.prompt.IPromptContext;
import com.ai.plug.core.context.prompt.PromptContextFactory;
import com.ai.plug.core.context.resource.IResourceContext;
import com.ai.plug.core.context.resource.ResourceContextFactory;
import com.ai.plug.core.context.root.IRootContext;
import com.ai.plug.core.context.root.RootContextFactory;
import com.ai.plug.core.context.tool.IToolContext;
import com.ai.plug.core.context.tool.ToolContextFactory;
import com.ai.plug.core.parser.tool.des.*;
import com.ai.plug.core.parser.tool.param.*;
import com.ai.plug.core.parser.tool.starter.AbstractStarter;
import com.ai.plug.core.parser.tool.starter.SingleStarter;
import com.ai.plug.core.register.complete.McpCompleteScanConfigurer;
import com.ai.plug.core.register.prompt.McpPromptScanConfigurer;
import com.ai.plug.core.register.resource.McpResourceScanConfigurer;
import com.ai.plug.core.register.tool.McpToolScanConfigurer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

import static com.ai.plug.common.constants.ConfigConstants.*;
import static com.ai.plug.core.annotation.ToolScan.FilterType.ANNOTATION;
import static com.ai.plug.core.annotation.ToolScan.ToolFilterType.META_ANNOTATION;

/**
 * @author 韩
 */
@Configuration
public class McpConfig {


    @Configuration
    @Conditional(Conditions.RootCondition.class)
    static class RootSpecificationConfiguration {
        /**
         * root 容器
         */
        @Bean
        public IRootContext rootContext() {
            return RootContextFactory.createRootContext();
        }

        /**
         * root 变化处理函数
         * @param rootContext root 容器
         * @return
         */
        @Bean
        public BiConsumer<McpSyncServerExchange, List<McpSchema.Root>> rootChangeConsumer(IRootContext rootContext) {
            return (exchange, roots) -> {
                rootContext.updateRoots(exchange, roots);
            };
        }
    }


    @Configuration
    @Conditional(Conditions.ToolCondition.class)
    @Import(ToolSpecificationConfiguration.AutoConfiguredToolScannerRegistrar.class)
    static class ToolSpecificationConfiguration {

        /**
         * 自动配置Tool 类扫描器
         */
        @Conditional(Conditions.IsInterfaceCondition.class)
        public static class AutoConfiguredToolScannerRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
            private BeanFactory beanFactory;

            private IToolContext toolContext;
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
                this.toolContext = this.beanFactory.getBean(IToolContext.class);
                Server2McpAutoConfiguration.logger.debug("Searching for tools of interfaces");


                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(McpToolScanConfigurer.class);

                String[] basePackages = AutoConfigurationPackages.get(this.beanFactory).toArray(new String[0]);
                builder.addPropertyValue("basePackages", basePackages);

                List<Class<?>> excludeClass = List.of(Deprecated.class, ToolNotScanForAuto.class);
                AnnotationAttributes[] excludeFilters = new AnnotationAttributes[excludeClass.size()];
                int index = 0;
                for (Class<?> clazz : excludeClass) {
                    HashMap<String, Object> excludeMap = new HashMap<>();
                    excludeMap.put("classes", clazz);
                    excludeMap.put("type", ANNOTATION);
                    excludeMap.put("value", clazz);
                    excludeFilters[index] = new AnnotationAttributes(excludeMap);
                    index++;
                }
                builder.addPropertyValue("excludeFilters", excludeFilters);

                HashMap<String, Object> includeMap = new HashMap<>();
                includeMap.put("classes", Controller.class);
                includeMap.put("type", ANNOTATION);
                includeMap.put("value", Controller.class);
                AnnotationAttributes[] includeFilters = new AnnotationAttributes[]{new AnnotationAttributes(includeMap)};
                builder.addPropertyValue("includeFilters", includeFilters);


                HashMap<String, Object> excludeToolMap = new HashMap<>();
                excludeToolMap.put("classes", new Class[]{Deprecated.class, ToolNotScanForAuto.class});
                excludeToolMap.put("type", META_ANNOTATION);
                excludeToolMap.put("value", new Class[]{Deprecated.class, ToolNotScanForAuto.class});
                ToolScan.ToolFilter[] excludeToolFilters= convertToToolFilters(new AnnotationAttributes[]{new AnnotationAttributes(excludeToolMap)});
                builder.addPropertyValue("excludeToolFilters", excludeToolFilters);

                HashMap<String, Object> includeToolMap = new HashMap<>();
                includeToolMap.put("classes", RequestMapping.class);
                includeToolMap.put("type", META_ANNOTATION);
                includeToolMap.put("value", RequestMapping.class);
                ToolScan.ToolFilter[] includeToolFilters = convertToToolFilters(new AnnotationAttributes[]{new AnnotationAttributes(includeToolMap)});
                builder.addPropertyValue("includeToolFilters", includeToolFilters);
                builder.addPropertyValue("toolContext", toolContext);


                builder.setRole(2);
                registry.registerBeanDefinition(McpToolScanConfigurer.class.getName(), builder.getBeanDefinition());

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
                        public Class<? extends Annotation> annotationType() {
                            return ToolScan.ToolFilter.class;
                        }
                    };
                }
                return toolFilters;
            }


        }


        /**
         * tool 容器
         */
        @Bean
        public IToolContext toolContext() {
            return ToolContextFactory.createToolContext();
        }
        /**
         * 工具定义构建器 ToolDefinitionBuilder
         * @param desParserList 描述解析器 des parser
         * @param paramParserList 参数解析器 param parser
         * @param starter 启动器
         * @return 工具定义构建器 ToolDefinitionBuilder
         */
        @Bean
        public ToolDefinitionBuilder toolDefinitionBuilder(List<AbstractDesParser> desParserList, List<AbstractParamParser> paramParserList, AbstractStarter starter) {
            return new ToolDefinitionBuilder(desParserList, paramParserList, starter);
        }

        @Bean
        public AbstractStarter starter() {
            return new SingleStarter();
        }
        @Bean
        @ConditionalOnParser(value = "MCPTOOL", type = AbstractDesParser.class)
        @Order(0)
        public AbstractDesParser mcpToolDesParser() {
            return new McpToolDesParser();
        }
        @Bean
        @ConditionalOnParser(value = "TOOL", type = AbstractDesParser.class)
        @Order(1)
        public AbstractDesParser toolDesParser() {
            return new ToolDesParser();
        }

        @Bean
        @ConditionalOnParser(value = "JACKSON", type = AbstractDesParser.class)
        @Order(2)
        public AbstractDesParser jacksonDesParser() {
            return new JacksonDesParser();
        }
        @Bean
        @ConditionalOnParser(value = "JAVADOC", type = AbstractDesParser.class)
        @Order(3)
        public AbstractDesParser javaDocDesParser() {
            return new JavaDocDesParser();
        }
        @Bean
        @ConditionalOnParser(value = "SWAGGER3", type = AbstractDesParser.class)
        @Order(4)
        public AbstractDesParser swagger3DesParser() {
            return new Swagger3DesParser();
        }
        @Bean
        @ConditionalOnParser(value = "SWAGGER2", type = AbstractDesParser.class)
        @Order(5)
        public AbstractDesParser swagger2DesParser() {
            return new Swagger2DesParser();
        }





        @Bean
        @ConditionalOnParser(value = "MCPTOOL", type = AbstractParamParser.class)
        @Order(0)
        public McpToolParamParser mcpToolParamParser() {
            return new McpToolParamParser();
        }

        @Bean
        @ConditionalOnParser(value = "TOOL", type = AbstractParamParser.class)
        @Order(1)
        public ToolParamParser toolParamParser() {
            return new ToolParamParser();
        }

        @Bean
        @ConditionalOnParser(value = "SPRINGMVC", type = AbstractParamParser.class)
        @Order(2)
        public AbstractParamParser mvcParamParser() {
            return new MvcParamParser();
        }
        @Bean
        @ConditionalOnParser(value = "JACKSON", type = AbstractParamParser.class)
        @Order(3)
        public AbstractParamParser jacksonParamParser() {
            return new JacksonParamParser();
        }

        @Bean
        @ConditionalOnParser(value = "JAVADOC", type = AbstractParamParser.class)
        @Order(4)
        public AbstractParamParser javaDocParamParser() {
            return new JavaDocParamParser();
        }
        @Bean
        @ConditionalOnParser(value = "SWAGGER3", type = AbstractParamParser.class)
        @Order(5)
        public AbstractParamParser swagger3ParamParser() {
            return new Swagger3ParamParser();
        }

        @Bean
        @ConditionalOnParser(value = "SWAGGER2", type = AbstractParamParser.class)
        @Order(6)
        public AbstractParamParser swagger2ParamParser() {
            return new Swagger2ParamParser();
        }


    }

    @Configuration
    @Conditional(Conditions.ResourceCondition.class)
    @Import(ResourceSpecificationConfiguration.AutoConfiguredResourceScannerRegistrar.class)
    static class ResourceSpecificationConfiguration {

        /**
         * 自动配置Resource 类扫描器
         */
        public static class AutoConfiguredResourceScannerRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
            private BeanFactory beanFactory;
            private IResourceContext resourceContext;
            public AutoConfiguredResourceScannerRegistrar() {

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
                this.resourceContext = this.beanFactory.getBean(IResourceContext.class);

                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(McpResourceScanConfigurer.class);

                String[] basePackages = AutoConfigurationPackages.get(this.beanFactory).toArray(new String[0]);
                builder.addPropertyValue("basePackages", basePackages);

                HashMap<String, Object> excludeMap = new HashMap<>(10);
                excludeMap.put("classes", Deprecated.class);
                excludeMap.put("type", McpResourceScan.FilterType.ANNOTATION);
                excludeMap.put("value", Deprecated.class);
                AnnotationAttributes[] excludeFilters = new AnnotationAttributes[]{new AnnotationAttributes(excludeMap)};
                builder.addPropertyValue("excludeFilters", excludeFilters);
                builder.addPropertyValue("resourceContext", this.resourceContext);

                builder.setRole(2);
                registry.registerBeanDefinition(McpResourceScanConfigurer.class.getName(), builder.getBeanDefinition());

            }


        }

        /**
         * resource 容器
         */
        @Bean
        public IResourceContext resourceContext() {
            return ResourceContextFactory.createResourceContext();
        }
    }

    @Configuration
    @Conditional(Conditions.PromptCondition.class)
    @Import(PromptSpecificationConfiguration.AutoConfiguredPromptScannerRegistrar.class)
    static class PromptSpecificationConfiguration {
        /**
         * 自动配置Prompt 类扫描器
         */
        public static class AutoConfiguredPromptScannerRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
            private BeanFactory beanFactory;

            private IPromptContext promptContext;

            public AutoConfiguredPromptScannerRegistrar() {
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
                this.promptContext = this.beanFactory.getBean(IPromptContext.class);

                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(McpPromptScanConfigurer.class);

                String[] basePackages = AutoConfigurationPackages.get(this.beanFactory).toArray(new String[0]);
                builder.addPropertyValue("basePackages", basePackages);

                HashMap<String, Object> excludeMap = new HashMap<>(10);
                excludeMap.put("classes", Deprecated.class);
                excludeMap.put("type", McpPromptScan.FilterType.ANNOTATION);
                excludeMap.put("value", Deprecated.class);
                AnnotationAttributes[] excludeFilters = new AnnotationAttributes[]{new AnnotationAttributes(excludeMap)};
                builder.addPropertyValue("excludeFilters", excludeFilters);
                builder.addPropertyValue("promptContext", promptContext);

                builder.setRole(2);
                registry.registerBeanDefinition(McpPromptScanConfigurer.class.getName(), builder.getBeanDefinition());

            }


        }

        /**
         * prompt 容器
         */
        @Bean
        public IPromptContext promptContext() {
            return PromptContextFactory.createPromptContext();
        }
    }


    @Configuration
    @Conditional(Conditions.CompleteCondition.class)
    @Import(CompleteSpecificationConfiguration.AutoConfiguredCompleteScannerRegistrar.class)
    static class CompleteSpecificationConfiguration {

        /**
         * 自动配置Complete 类扫描器
         */
        public static class AutoConfiguredCompleteScannerRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
            private BeanFactory beanFactory;

            private ICompleteContext completeContext;

            public AutoConfiguredCompleteScannerRegistrar() {
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
                this.completeContext = this.beanFactory.getBean(ICompleteContext.class);

                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(McpCompleteScanConfigurer.class);

                String[] basePackages = AutoConfigurationPackages.get(this.beanFactory).toArray(new String[0]);
                builder.addPropertyValue("basePackages", basePackages);

                HashMap<String, Object> excludeMap = new HashMap<>(10);
                excludeMap.put("classes", Deprecated.class);
                excludeMap.put("type", McpCompleteScan.FilterType.ANNOTATION);
                excludeMap.put("value", Deprecated.class);
                AnnotationAttributes[] excludeFilters = new AnnotationAttributes[]{new AnnotationAttributes(excludeMap)};
                builder.addPropertyValue("excludeFilters", excludeFilters);
                builder.addPropertyValue("completeContext", this.completeContext);

                builder.setRole(2);
                registry.registerBeanDefinition(McpCompleteScanConfigurer.class.getName(), builder.getBeanDefinition());

            }


        }

        /**
         * complete 容器
         */
        @Bean
        public ICompleteContext completeContext() {
            return CompleteContextFactory.createCompleteContext();
        }

    }






}