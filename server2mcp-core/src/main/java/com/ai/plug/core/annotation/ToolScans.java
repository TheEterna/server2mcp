package com.ai.plug.core.annotation;

import com.ai.plug.core.register.tool.ToolScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author: han
 * time: 2025/04/2025/4/2 22:29
 * des:
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ToolScanRegistrar.RepeatingRegistrar.class})
public @interface ToolScans {

    ToolScan[] value();

}
