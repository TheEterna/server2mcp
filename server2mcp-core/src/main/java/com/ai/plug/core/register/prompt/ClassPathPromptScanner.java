package com.ai.plug.core.register.prompt;

import com.ai.plug.core.context.prompt.IPromptContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;


/**
 * @author han
 * time: 2025/6/13 11:02
 */

public class ClassPathPromptScanner extends ClassPathBeanDefinitionScanner {


    public ClassPathPromptScanner(BeanDefinitionRegistry registry,
                                  IPromptContext promptContext) {
        super(registry, false);
        setBeanNameGenerator(new PromptBeanNameGenerator(promptContext));
    }


    public static class PromptBeanNameGenerator extends AnnotationBeanNameGenerator {
        private final IPromptContext promptContext;
        public PromptBeanNameGenerator(IPromptContext promptContext) {
            this.promptContext = promptContext;
        }

        @Override
        public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
            String beanName = super.generateBeanName(definition, registry);

            this.promptContext.addPrompt(beanName);

            return beanName;
        }
    }


}
