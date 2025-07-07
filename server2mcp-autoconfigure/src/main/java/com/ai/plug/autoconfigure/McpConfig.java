package com.ai.plug.autoconfigure;

import com.ai.plug.autoconfigure.annotation.ToolNotScanForAuto;
import com.ai.plug.autoconfigure.conditional.Conditions;
import com.ai.plug.core.annotation.McpCompleteScan;
import com.ai.plug.core.annotation.McpPromptScan;
import com.ai.plug.core.annotation.McpResourceScan;
import com.ai.plug.core.annotation.ToolScan;
import com.ai.plug.core.builder.ToolDefinitionBuilder;
import com.ai.plug.core.parser.tool.des.AbstractDesParser;
import com.ai.plug.core.parser.tool.param.AbstractParamParser;
import com.ai.plug.core.parser.tool.starter.AbstractStarter;
import com.ai.plug.core.register.complete.McpCompleteScanConfigurer;
import com.ai.plug.core.register.prompt.McpPromptScanConfigurer;
import com.ai.plug.core.register.resource.McpResourceScanConfigurer;
import com.ai.plug.core.register.tool.ToolScanConfigurer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;

import static com.ai.plug.common.constants.ConfigConstants.*;
import static com.ai.plug.core.annotation.ToolScan.FilterType.ANNOTATION;
import static com.ai.plug.core.annotation.ToolScan.ToolFilterType.META_ANNOTATION;

/**
 * @author 韩
 */
@Configuration
public class McpConfig {

    @Conditional(Conditions.IsInterfaceCondition.class)
    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_TOOL, name = ".enabled", havingValue = "true", matchIfMissing = true)
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

    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_RESOURCE, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public static class AutoConfiguredResourceScannerRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
        private BeanFactory beanFactory;

        // 无参构造器
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

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(McpResourceScanConfigurer.class);

            String[] basePackages = AutoConfigurationPackages.get(this.beanFactory).toArray(new String[0]);
            builder.addPropertyValue("basePackages", basePackages);

            HashMap<String, Object> excludeMap = new HashMap<>(10);
            excludeMap.put("classes", Deprecated.class);
            excludeMap.put("type", McpResourceScan.FilterType.ANNOTATION);
            excludeMap.put("value", Deprecated.class);
            AnnotationAttributes[] excludeFilters = new AnnotationAttributes[]{new AnnotationAttributes(excludeMap)};
            builder.addPropertyValue("excludeFilters", excludeFilters);

            builder.setRole(2);
            registry.registerBeanDefinition(McpResourceScanConfigurer.class.getName(), builder.getBeanDefinition());

        }


    }

    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_PROMPT, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public static class AutoConfiguredPromptScannerRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
        private BeanFactory beanFactory;

        // 无参构造器
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

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(McpPromptScanConfigurer.class);

            String[] basePackages = AutoConfigurationPackages.get(this.beanFactory).toArray(new String[0]);
            builder.addPropertyValue("basePackages", basePackages);

            HashMap<String, Object> excludeMap = new HashMap<>(10);
            excludeMap.put("classes", Deprecated.class);
            excludeMap.put("type", McpPromptScan.FilterType.ANNOTATION);
            excludeMap.put("value", Deprecated.class);
            AnnotationAttributes[] excludeFilters = new AnnotationAttributes[]{new AnnotationAttributes(excludeMap)};
            builder.addPropertyValue("excludeFilters", excludeFilters);

            builder.setRole(2);
            registry.registerBeanDefinition(McpPromptScanConfigurer.class.getName(), builder.getBeanDefinition());

        }


    }

    @ConditionalOnProperty(prefix = VARIABLE_PREFIX + '.' + VARIABLE_COMPLETE, name = ".enabled", havingValue = "true", matchIfMissing = true)
    public static class AutoConfiguredCompleteScannerRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
        private BeanFactory beanFactory;

        // 无参构造器
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

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(McpCompleteScanConfigurer.class);

            String[] basePackages = AutoConfigurationPackages.get(this.beanFactory).toArray(new String[0]);
            builder.addPropertyValue("basePackages", basePackages);

            HashMap<String, Object> excludeMap = new HashMap<>(10);
            excludeMap.put("classes", Deprecated.class);
            excludeMap.put("type", McpCompleteScan.FilterType.ANNOTATION);
            excludeMap.put("value", Deprecated.class);
            AnnotationAttributes[] excludeFilters = new AnnotationAttributes[]{new AnnotationAttributes(excludeMap)};
            builder.addPropertyValue("excludeFilters", excludeFilters);

            builder.setRole(2);
            registry.registerBeanDefinition(McpCompleteScanConfigurer.class.getName(), builder.getBeanDefinition());

        }


    }

    @Bean
    public ToolDefinitionBuilder toolDefinitionBuilder(List<AbstractDesParser> desParserList, List<AbstractParamParser> paramParserList, AbstractStarter starter) {
        return new ToolDefinitionBuilder(desParserList, paramParserList, starter);
    }
}