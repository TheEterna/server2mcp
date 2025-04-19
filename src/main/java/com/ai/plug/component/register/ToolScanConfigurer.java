package com.ai.plug.component.register;

import com.ai.plug.common.annotation.ToolScan;
import com.ai.plug.component.ToolContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: han
 * time: 2025/04/2025/4/15 00:35
 * des:
 */
public class ToolScanConfigurer implements BeanDefinitionRegistryPostProcessor, BeanFactoryPostProcessor {


    private String[] basePackages;
    private AnnotationAttributes[] excludeFilters;
    private AnnotationAttributes[] includeFilters;
    private ToolScan.ToolFilter[] excludeToolFilters;
    private ToolScan.ToolFilter[] includeToolFilters;

    public String[] getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }

    public AnnotationAttributes[] getExcludeFilters() {
        return excludeFilters;
    }

    public void setExcludeFilters(AnnotationAttributes[] excludeFilters) {
        this.excludeFilters = excludeFilters;
    }

    public AnnotationAttributes[] getIncludeFilters() {
        return includeFilters;
    }

    public void setIncludeFilters(AnnotationAttributes[] includeFilters) {
        this.includeFilters = includeFilters;
    }

    public ToolScan.ToolFilter[] getExcludeToolFilters() {
        return excludeToolFilters;
    }

    public void setExcludeToolFilters(ToolScan.ToolFilter[] excludeToolFilters) {
        this.excludeToolFilters = excludeToolFilters;
    }

    public ToolScan.ToolFilter[] getIncludeToolFilters() {
        return includeToolFilters;
    }

    public void setIncludeToolFilters(ToolScan.ToolFilter[] includeToolFilters) {
        this.includeToolFilters = includeToolFilters;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {



        ToolContext.ToolRegisterDefinition toolRegisterDefinition = new ToolContext.ToolRegisterDefinition(includeToolFilters, excludeToolFilters);
        // 创建类路径扫描器
        ClassPathBeanDefinitionScanner scanner = new ClassPathToolScanner(registry, toolRegisterDefinition);


        // 排除过滤器
        if (!CollectionUtils.isEmpty(List.of(excludeFilters))) {

            for (AnnotationAttributes excludeFilter : excludeFilters) {
                Class<?>[] excludeClasses = excludeFilter.getClassArray("value");
                switch ((ToolScan.FilterType) excludeFilter.getEnum("type")) {
                    case CLASS -> {
                        // 如果是 class过滤器
                        scanner.addExcludeFilter(new DeclaredClassExcludeFilter(excludeClasses));
                    }
                    case ANNOTATION -> {
                        for (Class<?> excludeClass : excludeClasses) {
                            if (!Annotation.class.isAssignableFrom(excludeClass)) {
                                throw new IllegalArgumentException("The passed class: " + excludeClass.getName() + " is not an annotation type");
                            }
                            scanner.addExcludeFilter(new AnnotationTypeFilter((Class<? extends Annotation>) excludeClass));
                        }
                    }

                }
            }
        }
        // 包含过滤器
        if (!CollectionUtils.isEmpty(List.of(includeFilters))) {

            for (AnnotationAttributes includeFilter : includeFilters) {
                Class<?>[] includeClasses = includeFilter.getClassArray("value");
                switch ((ToolScan.FilterType) includeFilter.getEnum("type")) {
                    case CLASS ->
                        // 如果是 class过滤器
                            scanner.addIncludeFilter(new DeclaredClassExcludeFilter(includeClasses));
                    case ANNOTATION -> {
                        for (Class<?> excludeClass : includeClasses) {
                            if (!Annotation.class.isAssignableFrom(excludeClass)) {
                                throw new IllegalArgumentException("The passed class: " + excludeClass.getName() + " is not an annotation type");
                            }
                            scanner.addIncludeFilter(new AnnotationTypeFilter((Class<? extends Annotation>) excludeClass));
                        }
                    }
                }
            }

        }
        else {
            scanner.addIncludeFilter(new DeclaredClassExcludeFilter(true, false, Object.class));
        }

        // 执行扫描
        scanner.scan(this.basePackages);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {





    }
    private static class DeclaredClassExcludeFilter extends AbstractTypeHierarchyTraversingFilter {
        private final Set<String> classNames = new HashSet<>();

        public DeclaredClassExcludeFilter(boolean considerInherited, boolean considerInterfaces, Class<?>... sources) {
            super(considerInherited, considerInterfaces);
            for(int i = 0; i < sources.length; i++) {
                this.classNames.add(sources[i].getName());
            }
        }

        DeclaredClassExcludeFilter(Class<?>... sources) {
            super(false, false);

            for(int i = 0; i < sources.length; i++) {
                this.classNames.add(sources[i].getName());
            }

        }

        protected boolean matchClassName(String className) {
            return this.classNames.contains(className);
        }
    }



}
