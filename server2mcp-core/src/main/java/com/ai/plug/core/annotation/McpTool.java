package com.ai.plug.core.annotation;


import com.ai.plug.common.constants.*;
import com.ai.plug.core.spec.callback.tool.DefaultMcpCallToolResultConverter;
import com.ai.plug.core.spec.callback.tool.McpCallToolResultConverter;

import java.lang.annotation.*;

/**
 * Marks a method as a MCP Tool.
 * @author han
 * @time 2025/6/25 10:14
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpTool {

    /**
     * Unique identifier for the tool. If not provided, the method name will be used.
     */
    String name() default "";

    /**
     * Optional human-readable name of the tool for display purposes.
     */
    String title() default "";

    /**
     * The description of the tool. If not provided, the method name will be used.
     */
    String description() default "";

    /**
     * inputSchema: JSON Schema defining expected parameters
     * outputSchema: Optional JSON Schema defining expected output structure
     * annotations: optional properties describing tool behavior
     */

    String mineType() default MineTypeConstants.JSON_MIME_TYPE;


    /**
     * If true, the tool does not modify its environment
     */
    boolean readOnlyHint() default false;

    /**
     * If true, the tool may perform destructive updates
     */
    boolean destructiveHint() default false;

    /**
     * If true, repeated calls with same args have no additional effect
     */
    boolean idempotentHint() default false;

    /**
     * If true, repeated calls with same args have no additional effect
     */
    boolean openWorldHint() default false;

    /**
     * If true, tool interacts with external entities
     */
    boolean returnDirect() default false;

    /**
     * The converter to use for converting the tool's output to a CallToolResult.
     */
    Class<? extends McpCallToolResultConverter> converter() default DefaultMcpCallToolResultConverter.class;

    /**
     * MCP 协议 2025-11-25（SEP-973）新增：图标元数据。格式为 data URI 或 https URL，
     * 可选附带 MIME 类型与尺寸（"32x32,64x64"）。留空则不发送 icons 字段。
     */
    String[] icons() default {};

    /**
     * MCP 协议 2025-11-25 新增：Tool 级 _meta 字段，留空则不发送。
     */
    String metaJson() default "";
}
