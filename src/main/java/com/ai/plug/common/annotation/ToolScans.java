package com.ai.plug.common.annotation;

import com.ai.plug.component.register.ToolScanRegistrar;
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
