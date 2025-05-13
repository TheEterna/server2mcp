package com.ai.plug.component.register;

import com.ai.plug.common.annotation.ToolScan;
import com.ai.plug.common.annotation.ToolScans;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ToolScanRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar {

    private BeanFactory beanFactory;

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 获取 @ToolScan 注解的属性值
        AnnotationAttributes attributes = (AnnotationAttributes) importingClassMetadata.getAnnotationAttributes(ToolScan.class.getName());

        if (attributes != null) {
            registerBeanDefinitions(importingClassMetadata, attributes, registry);
        }
    }
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, AnnotationAttributes attributes, BeanDefinitionRegistry registry) {

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ToolScanConfigurer.class);


        AnnotationAttributes[] basePackages = attributes.getAnnotationArray("basePackages");
        if (!CollectionUtils.isEmpty(List.of(basePackages))) {
            builder.addPropertyValue("basePackages", basePackages);
        } else {
            builder.addPropertyValue("basePackages", getDefaultBasePackage(importingClassMetadata));
        }

        AnnotationAttributes[] excludeFilters = attributes.getAnnotationArray("excludeFilters");
        if (!CollectionUtils.isEmpty(List.of(basePackages))) {
            builder.addPropertyValue("excludeFilters", excludeFilters);
        }

        AnnotationAttributes[] includeFilters = attributes.getAnnotationArray("includeFilters");
        if (!CollectionUtils.isEmpty(List.of(includeFilters))) {
            builder.addPropertyValue("includeFilters", includeFilters);
        }

        ToolScan.ToolFilter[] excludeToolFilters = convertToToolFilters(attributes.getAnnotationArray("excludeToolFilters"));
        if (!CollectionUtils.isEmpty(List.of(excludeToolFilters))) {
            builder.addPropertyValue("excludeToolFilters", excludeToolFilters);
        }
        ToolScan.ToolFilter[] includeToolFilters = convertToToolFilters(attributes.getAnnotationArray("includeToolFilters"));
        if (!CollectionUtils.isEmpty(List.of(includeToolFilters))) {
            builder.addPropertyValue("includeAttributesArray", includeToolFilters);
        }
        builder.setRole(2);
        registry.registerBeanDefinition(generateBaseBeanName(importingClassMetadata, 0), builder.getBeanDefinition());
    }

    private static String generateBaseBeanName(AnnotationMetadata importingClassMetadata, int index) {
        String className = importingClassMetadata.getClassName();
        return className + "#" + ToolScanRegistrar.class.getSimpleName() + "#" + index;
    }
    private static String getDefaultBasePackage(AnnotationMetadata importingClassMetadata) {
        return ClassUtils.getPackageName(importingClassMetadata.getClassName());
    }

    // 类型转换用
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


    // 给ToolScans用
    public static class RepeatingRegistrar extends ToolScanRegistrar {
        public RepeatingRegistrar() {
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            AnnotationAttributes attributes = (AnnotationAttributes) importingClassMetadata.getAnnotationAttributes(ToolScans.class.getName());
            if (attributes != null) {
                AnnotationAttributes[] annotations = attributes.getAnnotationArray("value");
                for (int i = 0; i < annotations.length; i++) {
                    this.registerBeanDefinitions(importingClassMetadata, annotations[i], registry);
                }

            }
        }

    }



}    