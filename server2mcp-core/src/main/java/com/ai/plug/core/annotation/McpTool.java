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
     * If true, the tool's tool list is dynamic — clients should be informed of
     * additions/removals via notifications/tools/list_changed. Default
     * {@code true} so SDK listChanged notifications work out-of-the-box without
     * per-tool configuration. Set to {@code false} for static tool sets.
     */
    boolean listChanged() default true;

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

    /**
     * MCP 协议 2026-07-28 SEP-2322：工具响应 resultType。SDK 2.0 无字段化
     * （javap 实证），本项目通过 McpResultWriter 在 wire 层落地。默认
     * {@code "complete"} 表示普通结果；设为 {@code "input_required"} 表示
     * 工具会通过 InputRequiredResult 触发 MRTR 重试。留空表示走默认
     * 行为（即普通结果）。
     */
    String resultType() default "complete";

    /**
     * MCP 协议 2026-07-28 SEP-2549：缓存 ttlMs（毫秒）。负数或零表示不缓存。
     * 默认 0（不缓存）——保守默认以免误缓存破坏 LLM 调用结果一致性。
     */
    long ttlMs() default 0;

    /**
     * MCP 协议 2026-07-28 SEP-2549：缓存范围。可选 {@code "public"} 或
     * {@code "private"}（中间件可缓存 vs 私有端缓存）。留空则不发送。
     */
    String cacheScope() default "";

    /**
     * 自定义 wrapper key（默认 {@code "_cacheable"}）。仅当 ttlMs 或
     * cacheScope 非默认时使用；某些集成期望不同 wrapper key 时可覆盖。
     */
    String cacheWrapperKey() default "_cacheable";

    /**
     * MCP 协议扩展（多租户隔离）：允许访问本工具的租户 ID 列表。
     * <ul>
     *   <li>空数组（默认）= 所有已认证租户都可见。零配置迁移；</li>
     *   <li>非空 = 仅白名单内的租户 ID 能在 {@code tools/list} 看到
     *       本工具、能在 {@code tools/call} 调用本工具；</li>
     *   <li>匹配规则：精确字符串相等（区分大小写）。</li>
     * </ul>
     *
     * <p>租户 ID 解析由 {@code com.ai.plug.core.tenant.TenantResolver}
     * SPI 实现，默认从 {@code X-Mcp-Tenant} HTTP header 读取。
     * 调用阶段（{@code tools/call}）会再次校验，防止绕过 list 直接
     * 调 ID 触发越权。
     *
     * <p>优先级：{@link #denyAll()} &gt; {@link #tenants()}。
     * 当 {@code denyAll=true} 时，本工具对所有租户隐藏（包括 admin），
     * 适用于内部/平台级工具。
     */
    String[] tenants() default {};

    /**
     * 当为 {@code true} 时，工具对所有租户隐藏（含 admin 角色）。
     * 用于内部/平台级工具——例如健康检查、缓存预热、admin 维护接口。
     * 优先级高于 {@link #tenants()}。
     */
    boolean denyAll() default false;
}
