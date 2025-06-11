package com.ai.plug.core.annotation;

import com.ai.plug.core.register.resource.McpResourceScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author 韩
 * time: 2025/6/3 3:18
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({McpResourceScanRegistrar.RepeatingRegistrar.class})
public @interface McpResourceScans {

    McpResourceScan[] value();

}
