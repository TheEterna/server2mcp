package com.ai.plug.core.register.complete;

import com.ai.plug.core.context.CompleteContext;
import com.ai.plug.core.register.complete.ClassPathCompleteScanner;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

/**
 * @author han
 * @time 2025/6/16 17:08
 */

public class ClassPathCompleteScanner extends ClassPathBeanDefinitionScanner {

    public ClassPathCompleteScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        setBeanNameGenerator(new ClassPathCompleteScanner.CompleteBeanNameGenerator());
    }


    public static class CompleteBeanNameGenerator extends AnnotationBeanNameGenerator {
        public CompleteBeanNameGenerator() {
        }

        @Override
        public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
            String beanName = super.generateBeanName(definition, registry);

            CompleteContext.addComplete(beanName);

            return beanName;
        }
    }}
