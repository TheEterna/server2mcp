package com.ai.plug.core.annotation;

import com.ai.plug.core.register.prompt.McpPromptScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author 韩
 * time: 2025/6/13 3:37
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({McpPromptScanRegistrar.RepeatingRegistrar.class})
public @interface McpPromptScans {
    McpPromptScan[] value();
}
