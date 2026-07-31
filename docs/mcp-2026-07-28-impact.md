# MCP 协议 2026-07-28 对本项目的影响面归档

> 立宪日：2026-07-31（自检与立项已结束）
> 维护者：CEO（Han）
> 关联：战役计划 `docs/plan-MCP协议全面集成-2026-07-30.md` · Phase 2

## 一、为什么 Java 生态到不了 2026-07-28

MCP 协议 2026-07-28（协议诞生以来最大改版）发布于 2026-07-28。
截至本文档日，**Java 生态没有任何 SDK 支持该版本**：

| 组件 | 最新正式版 | 实现协议 | 是否支持 2026-07-28 |
|---|---|---|---|
| MCP Java SDK | 2.0.0（2026-06-11） | 2025-11-25 | ❌ |
| Spring AI | 2.0.0（2026-06-12） | 2025-11-25 | ❌ |
| TypeScript SDK | 最新 | 2026-07-28 | ✅（Tier 1） |
| Python SDK | 最新 | 2026-07-28 | ✅（Tier 1） |
| Go SDK | 最新 | 2026-07-28 | ✅（Tier 1） |
| C# SDK | 最新 | 2026-07-28 | ✅（Tier 1） |
| Rust SDK | beta | 2026-07-28 | ⚠️ beta |

**硬约束**：api2mcp4j 作为 MCP Java SDK + Spring AI 的上层封装，不实现协议传输层，因此只能跟随 SDK 演进。Java SDK 一日不跟进 2026-07-28，本项目一日不能完整对接该版本。

## 二、2026-07-28 每个变更对本项目的具体影响

下表按本项目的实现面分类（注解 / Context / Provider / Callback / 注入参数），逐项评估。

### 2.1 已移除（REMOVED）— 本项目需要删除对应支持

| 协议变更 | 本项目现状 | 影响 |
|---|---|---|
| **移除 initialize/initialized 握手**（SEP-2575） | SDK 已无握手调用，本项目未涉及 | 0 |
| **移除 `Mcp-Session-Id` header** | SDK 已不在协议层维护 session | 0 |
| **移除 `ping` RPC** | SDK 已移除 | 0（无需处理） |
| **移除 `logging/setLevel`** | SDK 已移除 log level 协商；本项目 `McpLogger` 仍支持 setLevel（SDK 2.0 已废止该 RPC） | **需审查**：`McpLogger.setLevel(...)` 调用是否仍可达？若 SDK 侧已彻底移除 server 端 setLevel 实现，调用将无效——**等 Java SDK 跟进后**才能定论 |
| **移除 `notifications/roots/list_changed`** | SDK 已移除；本项目 `McpRoot` 仍提供 RootCapabilities | **低风险**：`McpRoot` 仅是给用户方法注入的 Root 抽象，移除 root_change 通知不影响读取 |
| **移除 HTTP GET endpoint + `resources/subscribe`/`unsubscribe`** | SDK 已移除 | 0（本项目未用 SSE 订阅） |
| **移除 SSE event id / Last-Event-ID**（断流重传） | SDK 已移除 | 0 |

### 2.2 弃用（DEPRECATED）— 12 个月观察期，需提前规整

| 协议变更 | 本项目现状 | 影响 |
|---|---|---|
| **Roots / Sampling / Logging 三个特性**（SEP-2577） | `McpRoot` / `McpSampling` / `McpLogger` 三个注入参数；`McpResourceScan` / `McpPromptScan` 等也可能引用 | **大影响**——3 个核心特性按计划被弃用。SDK 2.0 仍完整实现，但 2026-07-28 起新实现应不再添加。本项目作为库**保留**接口即可，无需立即删——但**文档需明确提示用户**：2026-07-28 后这些参数将以 no-op 或抛错行为运行。**改动时机：等 Java SDK 跟进 2026-07-28 后**做大规模重设计 |
| **HTTP+SSE transport**（自 2025-03-26 deprecated） | 本项目依赖 Spring AI starter，传输由其决定 | **低影响**：Spring AI 2.0 默认 Streamable HTTP，本项目不直接接触传输 |
| **`includeContext` `"thisServer"` / `"allServers"`**（sampling） | 本项目 `McpSampling` 实现不显式设置 includeContext；走默认 | **0** |

### 2.3 新增（NATIVE ADDED）— 本项目当前不支持的协议能力

| 协议变更 | 本项目现状 | 影响 |
|---|---|---|
| **MRTR（Multi Round-Trip Requests）**——服务器返回 `resultType: input_required` 让客户端回答后再重试（SEP-2322） | 不支持；本项目工具调用一次性完成 | **大影响**——长期工具的交互模式彻底改变。等 SDK 跟进后，本项目需考虑：长任务分阶段 + 状态机 |
| **`server/discover` RPC**（SEP-2575）— 服务器宣告协议版本与能力 | 不支持；SDK 未暴露 | **小影响**——服务器端实现简单，等 SDK 暴露后再加 |
| **`subscriptions/listen` 单流订阅**（SEP-2575）— 替代 GET endpoint + subscribe | 不支持；未实现变更通知 | **大影响**——服务器需要主动推送变更才能让客户端感知。**等 SDK 跟进后**需要支持 `tools/listChanged` / `resources/listChanged` 通知触发 |
| **Tasks 扩展** `io.modelcontextprotocol/tasks`（SEP-2663）— 长任务通过 `tasks/get` 轮询 + `tasks/update` 交互 | 不支持；SDK 未实现 | **大影响**——长期任务模式。**等 SDK 跟进后**才能实装 |
| **MCP Apps 扩展** | 不支持 | **待评估**——是否引入到本项目要看用户场景 |
| **CacheableResult（ttlMs + cacheScope）**（SEP-2549）— 列表结果带可缓存性提示 | 不支持；列表结果无缓存元数据 | **中影响**——等 SDK 暴露 ListToolsResult/PaginatedResult 的可缓存字段后再加 |
| **`resultType` 必填字段**（SEP-2322）— 所有结果新增 `"complete"` / `"input_required"` 区分 | SDK 2.0 尚未支持 | **等 SDK 跟进** |
| **Errors `-32020`-`-32099` 重新分配**（#2858 等） | SDK 2.0 沿用旧编号 | **等 SDK 跟进** |
| **`extensions` 字段** | 不支持 | **小影响**——客户端能力声明字段，本项目 server 端读不到 |
| **OpenTelemetry `_meta` 约定**（SEP-414） | 不支持 | **小影响**——可加 4 行代码透传 traceparent / tracestate / baggage |
| **标准请求头 `Mcp-Method` / `Mcp-Name`（SEP-2243）** | 依赖传输层 SDK | **0**（本项目不接触传输层） |
| **`x-mcp-header` 工具参数头传递**（SEP-2243） | 不支持 | **小影响**——可让工具方法从某个特殊参数读取 |
| **RFC 9207 issuer 校验 / Client ID Metadata Documents**（SEP-2468 / #2858） | 与授权相关；本项目未接触 auth | **0** |

## 三、本项目策略

### 3.1 已通过本战役覆盖（Phase 1 已完成）

- ✅ outputSchema 实际发送
- ✅ Tool Icons / `_meta` / Annotations 补齐
- ✅ elicitation URL 模式
- ✅ Progress 注入
- ✅ deprecated API 清理（victools / Jackson 3 / ElicitRequest.builder / `@Nullable`）
- ✅ JSpecify 迁移

### 3.2 物理不可达（等 Java SDK 跟进 2026-07-28 才能动）

- ✅ ~~ToolPagination~~ — 实装为 `com.ai.plug.core.spec.pagination.PaginatedLists`（commit 81dbfe2）：parseOffset/formatOffset/clampPageSize/slice + 三个 List*Result 工厂。SDK 2.0 的 ListToolsResult/ListResourcesResult/ListPromptsResult 均接受 nextCursor 字段，框架级工具让用户在自定义 MCP server 中调用。本项目自身 Provider 不默认切片（属路由策略决定，应由调用方决定）
- ✅ ~~subscriptions/listen 变更通知~~ — 实装为 `com.ai.plug.core.spec.capabilities.{ChangeNotifications, ServerCapabilitiesFactory}`（commit 4f03ea9）。SDK 2.0 已暴露 `McpSyncServer.notifyToolsListChanged/ResourcesListChanged/PromptsListChanged/ResourcesUpdated`（async Mono 同形），但 `McpServer.SyncSpecification.capabilities(...)` 接受外部 `ServerCapabilities`。提供工具类供用户接入。**注**：Spring AI 2.0 starter 已自带 `McpServerChangeNotificationProperties`（含 `tool/resource/prompt ChangeNotification` 三布尔）——用户在自己 starter 配置 `spring.ai.mcp.server.change-notification.*` 即可 0 工具代码启用，本项目工具类只是备用入口
- ✅ ~~server identity 图标~~ — 实装为 `com.ai.plug.core.spec.implementation.ServerInfoFactory`（commit c31d5b1）：parseIcon 与完整 createFull(name, version, title, description, icons, websiteUrl)。SDK 2.0 `McpSchema.Implementation` 现支持 icons/websiteUrl（协议 2025-11-25）
- ✅ ~~_meta 工具 + OTel trace 透传~~ — 实装为 `com.ai.plug.core.spec.meta.MetaUtils`（commit 646b230）：forwardTraceContext 透传 traceparent/tracestate/baggage（SEP-414），merge + 字符串常量集中
- ❌ MRTR（resultType / InputRequiredResult）
- ❌ Tasks 扩展
- ❌ server/discover
- ❌ CacheableResult（ttlMs + cacheScope）
- ❌ 标准请求头 Mcp-Method / Mcp-Name / x-mcp-header

### 3.3 软影响（当前无须动，但需监控）

- 🔄 Roots / Sampling / Logging 弃用——保留实现，文档明确 no-op 警告
- 🔄 MCP-Protocol-Version header 演进——SDK 接管

## 四、SDK 跟踪机制

### 4.1 检查脚本

`scripts/check-mcp-sdk-version.sh`（已新增）扫描 ~/.m2 与 Maven Central 上的 MCP SDK 版本：

- 当前实际版本 < 2.x → 不支持 2026-07-28，无需提示
- 当前 ≥ 2.x 且 changelog 出现「stateless」/「MRTR」字样 → 启动日志提示升级并附本影响面文档链接
- 当前 ≥ 3.0.0 → 自动判定已支持 2026-07-28，启用相关 Phase 2 战役

### 4.2 触发升级战役的判据

任一满足即启动 Phase 2：

1. Maven Central 上 `io.modelcontextprotocol.sdk:mcp-core` 出现 ≥ 3.0.0 正式版且实现版本包含 2026-07-28
2. Spring AI 3.x 整合 MCP SDK 3.x
3. 协议侧再有 minor 版（如 2026-09-15）但 Java 仍滞后——考虑本项目是否自实现 transport 层

## 五、本文档维护者

更新时机：
- 每当 MCP SDK 新版发布（每 6 周）—— 检视变更日志，更新第二节
- 每当 Java SDK 支持 2026-07-28 — 启动 Phase 2 战役，对照第二节 3.2 列表逐项实装
- 每当本项目新增 MCP 特性 — 在第一节补充实现映射