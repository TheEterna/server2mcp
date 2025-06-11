package com.ai.plug.autoconfigure.conditional;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 韩
 * time: 2025/5/16 12:37
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(Conditions.ParserCondition.class)
public @interface ConditionalOnParser {

    Class<?> type();

    String value();
}
