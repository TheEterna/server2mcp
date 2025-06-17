package com.ai.plug.core.annotation;

import com.ai.plug.core.register.complete.McpCompleteScanRegistrar;
import com.ai.plug.core.register.prompt.McpPromptScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author han
 * @time 2025/6/16 17:06
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({McpCompleteScanRegistrar.RepeatingRegistrar.class})
public @interface McpCompleteScans {
    McpCompleteScan[] value();
}
