package com.ai.plug.core.register.complete;

import com.ai.plug.core.annotation.McpCompleteScan;
import com.ai.plug.core.register.complete.ClassPathCompleteScanner;
import com.ai.plug.core.spring.filter.DeclaredClassExcludeFilter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * @author han
 * @time 2025/6/16 17:07
 */

public class McpCompleteScanConfigurer implements BeanDefinitionRegistryPostProcessor {

    private String[] basePackages;
    private AnnotationAttributes[] excludeFilters;
    private AnnotationAttributes[] includeFilters;

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



    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        // 创建类路径扫描器
        ClassPathBeanDefinitionScanner scanner = new ClassPathCompleteScanner(registry);

        // 排除过滤器
        if (excludeFilters != null && excludeFilters.length != 0 && !CollectionUtils.isEmpty(List.of(excludeFilters))) {

            for (AnnotationAttributes excludeFilter : excludeFilters) {
                Class<?>[] excludeClasses = excludeFilter.getClassArray("value");
                switch ((McpCompleteScan.FilterType) excludeFilter.getEnum("type")) {
                    case CLASS:
                        // 如果是 class过滤器
                        scanner.addExcludeFilter(new DeclaredClassExcludeFilter(excludeClasses));
                        break;
                    case ANNOTATION:
                        for (Class<?> excludeClass : excludeClasses) {
                            if (!Annotation.class.isAssignableFrom(excludeClass)) {
                                throw new IllegalArgumentException("The passed class: " + excludeClass.getName() + " is not an annotation type");
                            }
                            scanner.addExcludeFilter(new AnnotationTypeFilter((Class<? extends Annotation>) excludeClass));
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " +  excludeFilter.getEnum("type"));
                }
            }
        }
        // 包含过滤器
        if (includeFilters != null && includeFilters.length != 0 && !CollectionUtils.isEmpty(List.of(includeFilters))) {

            for (AnnotationAttributes includeFilter : includeFilters) {
                Class<?>[] includeClasses = includeFilter.getClassArray("value");
                switch ((McpCompleteScan.FilterType) includeFilter.getEnum("type")) {
                    case CLASS:
                        // 如果是 class过滤器
                        scanner.addIncludeFilter(new DeclaredClassExcludeFilter(includeClasses));
                        break;
                    case ANNOTATION:
                        for (Class<?> excludeClass : includeClasses) {
                            if (!Annotation.class.isAssignableFrom(excludeClass)) {
                                throw new IllegalArgumentException("The passed class: " + excludeClass.getName() + " is not an annotation type");
                            }
                            scanner.addIncludeFilter(new AnnotationTypeFilter((Class<? extends Annotation>) excludeClass));
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " +  includeFilter.getEnum("type"));

                }
            }

        }
        else {
            scanner.addIncludeFilter(new DeclaredClassExcludeFilter(true, false, Object.class));
        }

        // 执行扫描
        scanner.scan(this.basePackages);
    }



}
