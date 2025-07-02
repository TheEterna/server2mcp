package com.ai.plug.core.register.resource;

import com.ai.plug.core.context.ResourceContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;


/**
 * @author han
 * time: 2025/04/2025/4/10 15:49
 * des:
 */

public class ClassPathResourceScanner extends ClassPathBeanDefinitionScanner {


    public ClassPathResourceScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        setBeanNameGenerator(new ResourceBeanNameGenerator());
    }


    public static class ResourceBeanNameGenerator extends AnnotationBeanNameGenerator {
        public ResourceBeanNameGenerator() {
        }

        @Override
        public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
            String beanName = super.generateBeanName(definition, registry);

            ResourceContext.addResource(beanName);

            return beanName;
        }
    }


}
