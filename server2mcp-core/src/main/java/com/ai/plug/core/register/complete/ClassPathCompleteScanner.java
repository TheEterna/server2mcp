package com.ai.plug.core.register.complete;

import com.ai.plug.core.context.complete.CompleteContext;
import com.ai.plug.core.context.complete.ICompleteContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

/**
 * @author han
 * @time 2025/6/16 17:08
 */

public class ClassPathCompleteScanner extends ClassPathBeanDefinitionScanner {

    private ICompleteContext completeContext;
    public ClassPathCompleteScanner(BeanDefinitionRegistry registry, ICompleteContext completeContext) {
        super(registry, false);
        setBeanNameGenerator(new ClassPathCompleteScanner.CompleteBeanNameGenerator(completeContext));
    }


    public static class CompleteBeanNameGenerator extends AnnotationBeanNameGenerator {
        private ICompleteContext completeContext;
        public CompleteBeanNameGenerator(ICompleteContext completeContext) {
            this.completeContext = completeContext;
        }

        @Override
        public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
            String beanName = super.generateBeanName(definition, registry);

            completeContext.addComplete(beanName);

            return beanName;
        }
    }}
