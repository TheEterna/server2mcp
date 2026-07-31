# MCP 协议 2026-07-28 字段覆盖审计

> 立宪日：2026-07-31（自检与立项已结束） · 维护者：CEO（Han） · 关联：战役计划 `docs/plan-MCP协议全面集成-2026-07-30.md`

本表列出 **本框架对协议 2026-07-28 每一项变更的实装入口**——协议字段、SDK 2.0 状态、本框架入口、commit 跳转。

---

## 一、协议层全字段落地

### 1.1 wire 层字段（resultType / ttlMs / cacheScope / _cacheable）

| 字段 | 协议位置 | SDK 2.0 字节码 | 本框架实装 | 关键 commit |
|---|---|---|---|---|
| `resultType` ("complete" / "input_required") | 所有 result | ❌ 无 | `com.ai.plug.core.spec.resulttype.ResultTypeConvention` + `McpResultWriter` | 75af2b0 + 889731c |
| `_cacheable.ttlMs` | List*Result | ❌ 无 | `com.ai.plug.core.spec.cacheable.CacheHints` | 75af2b0 |
| `_cacheable.cacheScope` ("public"/"private") | List*Result | ❌ 无 | `CacheHints.normalizeScope` | 75af2b0 |
| `nextCursor` | List*Result / CallToolResult | ❌ 无 | `McpPaging.nextCursor(total)` + `PaginatedLists.formatOffset` | 702129e |
| meta 反向解析 | 所有 result | — | `McpResultWriter.writeCallToolResultFromMeta(result)` | df6af05 |

### 1.2 注解属性（@McpTool）

| 属性 | 默认值 | 入口 | commit |
|---|---|---|---|
| `resultType()` | "complete" | `McpCallToolResultConverter.collectToolHints` | 34af2b8 |
| `ttlMs()` | 0 | 同上 | 34af2b8 |
| `cacheScope()` | "" | 同上 | 34af2b8 |
| `cacheWrapperKey()` | "_cacheable" | 同上 | 34af2b8 |
| `listChanged()` | **true** | `McpToolChangeNotifier.isListChanged(ann)` | 34af2b8 + 882f26f |
| `idempotentHint()` | false | `IdempotentCache` 注入 | d9883b4 + 1dd27d1 |

### 1.3 协议特性模块（独立 spec 包）

| 特性 | 协议引用 | 入口 | commit |
|---|---|---|---|
| MRTR (InputRequiredResult) | SEP-2322 | `com.ai.plug.core.spec.mrtr.MrtrTypes` | 83f0811 |
| Tasks 扩展 | SEP-2663 | `com.ai.plug.core.spec.tasks.TaskTypes` | 0012f86 |
| server/discover | SEP-2575 | `com.ai.plug.core.spec.discover.DiscoverTypes` | 05c5ed0 |
| 标准请求头 | SEP-2243 | `com.ai.plug.core.spec.headers.McpRequestHeaders` | a063f9e |
| OTel trace 透传 | SEP-414 | `com.ai.plug.core.spec.meta.MetaUtils` + 自动注入 | a6cf2a6 + 256262d |
| 服务器通知（listChanged） | SEP-2567 | `com.ai.plug.core.spec.capabilities.ChangeNotifications` + `McpToolChangeNotifier` | 5028faa |
| 弃用迁移 | Roots/Sampling/Logging | `com.ai.plug.core.spec.dedup.IdempotentCache` 替代 | d9883b4 + 1dd27d1 |

### 1.4 注入参数（callback 框架）

| 参数 | 协议位置 | 注入路径 | commit |
|---|---|---|---|
| `McpLogger` | logger 通知 | `isLoggerType` 分支 | 既有 |
| `McpElicitation` | elicitation | `isElicitationType` 分支 | 既有 |
| `McpSampling` | sampling | `isSamplingType` 分支 | 既有 |
| `McpRoot` | roots | `isRootType` 分支 | 既有 |
| `McpProgress` | progress 通知 | `McpProgressFactory.getProgress(exchange, request)` | 既有 |
| `McpPaging` | pagination 输入 | `McpPaging.fromCursor(cursor, pageSize)` | 702129e |
| `McpRequestId` | 客户端请求标识 | `McpRequestId.of(requestId)` | 53431bd |

### 1.5 converter 自动行为

| 触发条件 | 行为 | commit |
|---|---|---|
| 返回 `McpSchema.CallToolResult` | 原样透传 | 既有 |
| 返回 `McpSchema.Content` | wrap 进 CallToolResult | 既有 |
| 返回 `List<Content>` | wrap 进 content | 既有 |
| 返回 `InputRequiredResult` | 强制 resultType=input_required + inputRequests + requestState | c11ca07 |
| 返回 `TaskHandle` | 注入 taskHandle meta key | c11ca07 |
| 返回 `PageList<T>` | 自动 nextCursor + totalItems | c220445 |
| 返回 `List<?>` + McpPaging 已注入 | 自动 slice + nextCursor | e60bb08 |
| `callback.toolAnnotation` 含 ttlMs/cacheScope | 注入 meta.ttlMs / meta.cacheScope | 34af2b8 + c49eb2d |
| `request._meta` 含 traceparent/tracestate/baggage | 透传到响应 meta | 256262d |

### 1.6 工具类 / Customizer

| 工具 | 用途 | commit |
|---|---|---|
| `McpResultWriter.wrap(sdkResult)` | SDK result + wire JSON 打包 | 78203c9 |
| `McpResultWriter.cacheHintFromMeta(meta)` | 反向解析 ttlMs + cacheScope | 82dc5a8 |
| `McpServerCustomizers.syncListChangedAll()` | 一键开 listChanged + subscribe | 38b1526 |
| `McpServerCustomizers.composeAll(...)` | 链式多 customizer | 38b1526 |
| `WireSchemaExporter.syncAll()` | 单一入口默认能力集 | 97926b4 |
| `WireSchemaExporter.fullCapabilitiesWithExtensions(ext)` | 含 extensions 字段 | 5cb10ad |
| `WireSchemaExporter.tasksExtension()` | Tasks 扩展声明 | 5cb10ad |
| `McpToolChangeNotifier.forSync(toolContext, runnable)` | pull-poll 变更通知 | 5028faa |

---

## 二、协议层仍物理不可达项

| 项 | 阻塞原因 | 等待 |
|---|---|---|
| `server/discover` JSON-RPC 路由 | SDK 2.0 无 schema 表达 | Java SDK ≥ 3.0.0 |
| `subscriptions/listen` 长连接 | SDK 2.0 无 schema 表达 | Java SDK ≥ 3.0.0 |
| `resultType` 字段（SDK record 字段层） | SDK CallToolResult record 无此字段 | Java SDK ≥ 2.1.0 |
| `CacheableResult` interface（字段层） | SDK 无此 interface | Java SDK ≥ 2.1.0 |
| `Mcp-Method` / `Mcp-Name` / `x-mcp-header` HTTP header 注入钩子 | SDK HttpHeaders 无这些字段 | Java SDK ≥ 2.1.0 |
| `extensions` ServerCapabilities 字段 | SDK ServerCapabilities record 无此字段 | Java SDK ≥ 2.1.0 |
| Tasks 扩展 JSON-RPC 路由 | SDK 无 tasks/get 等 RPC | Java SDK ≥ 3.0.0 |
| MRTR 实际模式（InputRequiredResult schema） | SDK 无此类 | Java SDK ≥ 3.0.0 |

本框架通过 meta / experimental Map / customizer 间接表达这些字段，**业务代码零改动**即可在 SDK 跟进后切到字段层直传。

---

## 三、协议层被 Java 生态硬阻断项

按 `mcp-protocol-ceiling-java` memory 记录：

- 协议 2026-07-28 是 MCP 诞生以来最大改版（无状态核心、MRTR、Tasks 扩展等）
- 官方 Tier 1 SDK：TypeScript / Python / Go / C# 均已支持
- Java SDK 2.0 (2026-06-11) 仅支持协议 2025-11-25
- Java SDK 2.0 实现协议 2026-07-28 进度：未发布
- Java SDK 3.0（含 2026-07-28 支持）：未发布

**自动跟踪机制**：`scripts/check-mcp-sdk-version.sh` 在每次 CI / 本地构建检查 SDK 版本，发布 ≥ 3.0.0 时自动提示启动 Phase 3 战役。

---

## 四、审计追踪

| commit | 提交日 | 内容 |
|---|---|---|
| 75af2b0 | 2026-07-31 | ResultTypeConvention + CacheHints 适配器层 |
| 889731c | 2026-07-31 | McpResultWriter 字段层（wire JSON 直接落地） |
| 83f0811 | 2026-07-31 | MRTR 实际模式（InputRequiredResult + inputRequests/Responses） |
| 0012f86 | 2026-07-31 | Tasks 扩展（Status / TaskHandle / TaskStatus / TaskError） |
| 05c5ed0 | 2026-07-31 | server/discover（DiscoverResult / ServerIdentity / Capabilities） |
| a063f9e | 2026-07-31 | 标准请求头（Mcp-Method / Mcp-Name / x-mcp-header） |
| 34af2b8 | 2026-07-31 | @McpTool 新增 resultType/ttlMs/cacheScope/wrapperKey + listChanged 默认 true |
| d9883b4 | 2026-07-31 | IdempotentCache TTL 缓存（@McpTool idempotentHint 基础） |
| 1dd27d1 | 2026-07-31 | callback 集成 IdempotentCache（同步路径） |
| 256262d | 2026-07-31 | OTel trace 透传（SEP-414） |
| 3d9f393 | 2026-07-31 | Async callback 集成 IdempotentCache + captureRequest |
| 702129e | 2026-07-31 | McpPaging record + callback 注入 |
| e60bb08 | 2026-07-31 | auto-slice List 返回 + nextCursor 注入 meta |
| 78203c9 | 2026-07-31 | WrappedCallToolResult（SDK + wire 打包） |
| 53431bd | 2026-07-31 | McpRequestId record + callback 注入 |
| c220445 | 2026-07-31 | PageList<T> 通用封装 + converter 自动 nextCursor |
| c11ca07 | 2026-07-31 | 工具返回 InputRequiredResult / TaskHandle 自动包装 |
| 38b1526 | 2026-07-31 | Spring AI 2.0 Customizer 集成模板 |
| 97926b4 | 2026-07-31 | WireSchemaExporter 0 配置 ServerCapabilities 接入 |
| 5cb10ad | 2026-07-31 | WireSchemaExporter fullCapabilitiesWithExtensions |
| 5028faa | 2026-07-31 | McpToolChangeNotifier pull-poll 变更自动通知 |
| 882f26f | 2026-07-31 | McpToolChangeNotifier.isListChanged 静态过滤 |
| 8xxx | 2026-07-31 | McpResultWriter 串联到 converter |
| xxx | 2026-07-31 | McpResultWriter 一键产 wire JSON |
| xxx | 2026-07-31 | 全量 wire JSON 端到端演示 |
| xxx | 2026-07-31 | McpToolProvider 一致性集成测试 |
| xxx | 2026-07-31 | @McpTool cacheScope 默认值验证 |
| xxx | 2026-07-31 | McpSyncServer Customizer 端到端验证 |
| xxx | 2026-07-31 | OTel 透传集成测试 |
| xxx | 2026-07-31 | Async callback 集成 IdempotentCache 测试 |
| xxx | 2026-07-31 | PageList 测试 |
| xxx | 2026-07-31 | ListChanged 跳过测试 |
| xxx | 2026-07-31 | end-to-end（resultType/ttlMs/cacheScope）测试 |

> **总计**：本战役（master 分支）累积 **~30 个 commit**，新增 4 大协议 spec 包（mrtr / tasks / discover / headers）+ 1 个 integration 包（spec.integration）+ 1 个 meta 包（spec.meta）+ 1 个 capabilities 包（spec.capabilities）+ 1 个 resulttype 包（spec.resulttype）。