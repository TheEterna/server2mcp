# ServerCapabilities 字段映射表（MCP 协议 2026-07-28）

> 立宪日：2026-07-31 · 维护者：CEO（Han） · 关联：`docs/mcp-2026-07-28-coverage.md`

本表精确列出 `McpSchema.ServerCapabilities` record 的每个字段（SDK 2.0 字节码实证）在协议 2026-07-28 的位置，**本框架如何填充**它，以及 **SDK 跟进后**字段层切换路径。

---

## 一、SDK 2.0 `ServerCapabilities` record 字段表

| # | 字段名 | 类型 | 协议 2026-07-28 含义 | SDK 2.0 字段层 | 本框架注入方式 |
|---|---|---|---|---|---|
| 1 | `CompletionCapabilities completions` | nullable | 完成能力 | ✅ 字段 | SDK 默认 + `ServerCapabilities.builder().completions()` |
| 2 | `Map<String,Object> experimental` | nullable | 实验性字段（协议 2026-07-28 别名 `extensions`） | ✅ 字段 | `Builder.experimental(map)` / `WireSchemaExporter.fullCapabilitiesWithExtensions` |
| 3 | `LoggingCapabilities logging` | nullable | 日志能力 | ✅ 字段 | `Builder.logging()` |
| 4 | `PromptCapabilities prompts` | nullable | Prompt 能力 | ✅ 字段 | `ServerCapabilitiesFactory.withPromptsListChanged()` |
| 5 | `ResourceCapabilities resources` | nullable | Resource 能力 | ✅ 字段 | `ServerCapabilitiesFactory.withResourcesListChanged()` |
| 6 | `ToolCapabilities tools` | nullable | Tool 能力 | ✅ 字段 | `ServerCapabilitiesFactory.withToolsListChanged()` |

**注**：协议 2026-07-28 minor #1 引入 `extensions` 字段——SDK 2.0 把它映射到 `experimental`（来源：协议`extensions` 字段归属见 §三）。

---

## 二、子结构字段表

### 2.1 `ToolCapabilities`

| 字段 | 类型 | 协议 2026-07-28 含义 | 本框架来源 |
|---|---|---|---|
| `Boolean listChanged` | nullable | tools 列表变更时是否通知 client | `ServerCapabilitiesFactory.withToolsListChanged()` 默认 true |

### 2.2 `ResourceCapabilities`

| 字段 | 类型 | 协议 2026-07-28 含义 | 本框架来源 |
|---|---|---|---|
| `Boolean subscribe` | nullable | 是否支持 resource 订阅 | `ServerCapabilitiesFactory.withResourcesListChanged()` 默认 true |
| `Boolean listChanged` | nullable | resources 列表变更时是否通知 client | 同上，默认 true |

### 2.3 `PromptCapabilities`

| 字段 | 类型 | 协议 2026-07-28 含义 | 本框架来源 |
|---|---|---|---|
| `Boolean listChanged` | nullable | prompts 列表变更时是否通知 client | `ServerCapabilitiesFactory.withPromptsListChanged()` 默认 true |

### 2.4 `LoggingCapabilities`

| 字段 | 类型 | 协议 2026-07-28 含义 | 本框架来源 |
|---|---|---|---|
| （无字段） | — | 只是 marker 表明 logging 能力存在 | `Builder.logging()` |

### 2.5 `CompletionCapabilities`

| 字段 | 类型 | 协议 2026-07-28 含义 | 本框架来源 |
|---|---|---|---|
| （无字段） | — | 只是 marker 表明 completion 能力存在 | `Builder.completions()` |

---

## 三、协议 2026-07-28 字段映射到 SDK 2.0 字段

| 协议字段 | SDK 2.0 字段 / 路径 | 本框架注入 |
|---|---|---|
| `capabilities.tools.listChanged` | `ServerCapabilities.tools.listChanged` | `ServerCapabilitiesFactory.withToolsListChanged()` |
| `capabilities.resources.listChanged` | `ServerCapabilities.resources.listChanged` | `ServerCapabilitiesFactory.withResourcesListChanged()` |
| `capabilities.resources.subscribe` | `ServerCapabilities.resources.subscribe` | 同上 |
| `capabilities.prompts.listChanged` | `ServerCapabilities.prompts.listChanged` | `ServerCapabilitiesFactory.withPromptsListChanged()` |
| `capabilities.logging` | `ServerCapabilities.logging` | `Builder.logging()` |
| `capabilities.completions` | `ServerCapabilities.completions` | `Builder.completions()` |
| `capabilities.extensions` | `ServerCapabilities.experimental` | `WireSchemaExporter.fullCapabilitiesWithExtensions(map)` |
| `capabilities.extensions["io.modelcontextprotocol/tasks"]` | `experimental.put("io.modelcontextprotocol/tasks", Map.of("version", "draft"))` | `WireSchemaExporter.tasksExtension()` |

---

## 四、本框架注入路径全景

### 4.1 注释层声明（@McpTool）

```java
@McpTool(
    name = "my-tool",
    resultType = "complete",      // → meta.resultType
    ttlMs = 60_000,             // → meta.ttlMs
    cacheScope = "private",      // → meta.cacheScope
    cacheWrapperKey = "_cacheable", // → meta.cacheWrapperKey
    listChanged = true          // → McpToolChangeNotifier.isListChanged
)
```

→ 调用链：`DefaultMcpCallToolResultConverter.collectToolHints(callback)` → 写入 `CallToolResult.meta`

### 4.2 Customizer 层（Spring AI 2.0 Bean）

```java
@Bean
public McpSyncServerCustomizer wire() {
    return WireSchemaExporter.syncAll();
}
```

→ 调用链：`McpServerCustomizers.syncListChangedAll()` → `spec.capabilities(factory.withListChangedAll())`

### 4.3 协议特性层（独立 spec 包）

| 协议特性 | 入口 | 注入路径 |
|---|---|---|
| MRTR (InputRequiredResult) | `MrtrTypes.InputRequiredResult` | 工具返回 → converter 自动包装 → meta.resultType=input_required + meta.inputRequests + meta.requestState |
| Tasks (TaskHandle) | `TaskTypes.TaskHandle` | 工具返回 → converter 自动包装 → meta.taskHandle |
| Tasks (TaskStatus) | `TaskTypes.TaskStatus` | 用于自定义 MCP server 处理 `tasks/get` RPC |
| server/discover | `DiscoverTypes.DiscoverResult` | 用于自定义 MCP server 处理 `server/discover` RPC |
| 标准请求头 | `McpRequestHeaders.forJsonRpcCall(method, name)` | 由 spring AI 自定义 transport 实现使用 |
| OTel trace | `MetaUtils.forwardTraceContext(meta)` | callback 入口自动转发 |
| listChanged 通知 | `ChangeNotifications.notifyXxxListChanged` | `McpToolChangeNotifier` 触发 |
| capabilities.extensions | `WireSchemaExporter.fullCapabilitiesWithExtensions(map)` | 注入 `experimental` Map |

---

## 五、SDK 跟进后（Java SDK ≥ 2.1.0）字段层切换路径

| 当前注入方式 | 跟进后等价 |
|---|---|
| `meta.resultType = "complete"` | `McpSchema.CallToolResult.Builder.resultType("complete")` |
| `meta.ttlMs = 60_000` | `McpSchema.CallToolResult.Builder.ttlMs(60_000L)` |
| `meta.cacheScope = "private"` | `McpSchema.CallToolResult.Builder.cacheScope("private")` |
| `meta.cacheWrapperKey = "_cacheable"` | `McpSchema.CallToolResult.Builder.cacheWrapperKey("_cacheable")` |
| `meta.inputRequests = [...]` | SDK 暴露 InputRequiredResult 类 |
| `meta.nextCursor` | `McpSchema.ListToolsResult.Builder.nextCursor(...)` |

**本框架迁移策略**（按计划）：

1. 把 `McpResultWriter.cacheHintFromMeta(meta)` 切到 SDK Builder 调用
2. 把 `DefaultMcpCallToolResultConverter.collectToolHints` 切到 SDK Builder 调用
3. 删除 `_cacheable` wrapper 概念（SDK 直接有 `_cacheable` 字段）
4. 测试无业务改动（行为完全一致）

---

## 六、未实装字段（SDK 2.0 字节码实证）

| 协议字段 | 状态 | 备注 |
|---|---|---|
| `serverInfo.title` | ✅ SDK 字段 | `ServerInfoFactory.create(name, version, title, desc)` |
| `serverInfo.description` | ✅ SDK 字段 | 同上 |
| `serverInfo.icons` | ✅ SDK 字段 | `ServerInfoFactory.createFull(...,List.of(Icon),...)` |
| `serverInfo.websiteUrl` | ✅ SDK 字段 | 同上 |
| `serverInfo.extensions` | ❌ SDK 无字段 | 协议 2026-07-28 minor #1，未实装 |
| `tools/*.listChanged` (per-tool) | ✅ SDK 字段 | `McpToolAnnotations` 字段 |
| `capabilities.extensions` (per-cap) | ❌ SDK 无字段 | 同上 |
| `tools/*.outputSchema` (per-tool) | ✅ SDK 字段 | `McpTool.builder().outputSchema(...)` |
| `tools/*.meta` (per-tool) | ✅ SDK 字段 | `McpTool.builder().meta(...)` |
| `resultType` 全局字段 | ❌ SDK record 无 | 协议 2026-07-28 SEP-2322 |
| `CacheableResult` interface | ❌ SDK 无 interface | 协议 2026-07-28 SEP-2549 |
| `extensions` 全局字段 | ❌ SDK record 无 | 协议 2026-07-28 minor #1 |

---

## 七、审计追踪

| 字段位置 | 审计 commit |
|---|---|
| `meta.resultType` | 75af2b0 + 889731c |
| `meta.ttlMs` / `meta.cacheScope` | 75af2b0 + 34af2b8 |
| `meta.inputRequests` | 83f0811 (MrtrTypes) |
| `meta.taskHandle` | 0012f86 (TaskTypes) |
| `meta.nextCursor` | 702129e (McpPaging) + c220445 (PageList) |
| `meta.traceparent` / `meta.tracestate` / `meta.baggage` | 256262d |
| `experimental` field | 5cb10ad (WireSchemaExporter) |
| `serverInfo.title` / `description` / `icons` / `websiteUrl` | 38b1526 (McServerCustomizers) + 53431bd |

> **建议**：每 6 周核对 MCP 官方 changelog 与本表——任何 SDK 2.1+ 暴露的新字段都应同步到「五、SDK 跟进后字段层切换路径」列表。