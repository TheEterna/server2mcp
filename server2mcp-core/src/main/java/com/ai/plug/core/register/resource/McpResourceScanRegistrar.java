package com.ai.plug.core.register.resource;

import com.ai.plug.core.annotation.McpResourceScan;
import com.ai.plug.core.annotation.McpResourceScans;
import com.ai.plug.core.context.resource.IResourceContext;
import com.ai.plug.core.utils.CustomToolUtil;
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

/**
 * @author 韩
 * time: 2025/6/3 8:38
 */

public class McpResourceScanRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
    private static int index = 0;
    private IResourceContext resourceContext;
    private BeanFactory beanFactory;

    public McpResourceScanRegistrar() {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 获取 @McpResourceScan 注解的属性值
        AnnotationAttributes attributes = (AnnotationAttributes) importingClassMetadata.getAnnotationAttributes(McpResourceScan.class.getName());

        if (attributes != null) {
            registerBeanDefinitions(importingClassMetadata, attributes, registry);
        }
    }
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, AnnotationAttributes attributes, BeanDefinitionRegistry registry) {

        this.resourceContext = beanFactory.getBean(IResourceContext.class);
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(McpResourceScanConfigurer.class);

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
        builder.addPropertyValue("resourceContext", this.resourceContext);

        builder.setRole(2);
        registry.registerBeanDefinition(generateBaseBeanName(importingClassMetadata, index++), builder.getBeanDefinition());
    }

    private static String generateBaseBeanName(AnnotationMetadata importingClassMetadata, int index) {
        return CustomToolUtil.generateBaseBeanName(importingClassMetadata, index);
    }
    private static String getDefaultBasePackage(AnnotationMetadata importingClassMetadata) {
        return ClassUtils.getPackageName(importingClassMetadata.getClassName());
    }



    /**
     * 给McpResourceScans用
     */
    public static class RepeatingRegistrar extends McpResourceScanRegistrar {


        public RepeatingRegistrar() {
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            AnnotationAttributes attributes = (AnnotationAttributes) importingClassMetadata.getAnnotationAttributes(McpResourceScans.class.getName());
            AnnotationAttributes[] annotations = attributes.getAnnotationArray("value");
            for (AnnotationAttributes annotation : annotations) {
                this.registerBeanDefinitions(importingClassMetadata, annotation, registry);
            }
        }

    }


}
